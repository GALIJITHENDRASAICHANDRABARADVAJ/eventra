package com.eventra.config;

import com.eventra.booking.BookingService;
import com.eventra.dto.BookingDtos.CreateBookingRequest;
import com.eventra.dto.BookingDtos.FoodLine;
import com.eventra.dto.PaymentDtos.InitiatePaymentRequest;
import com.eventra.entity.*;
import com.eventra.entity.enums.*;
import com.eventra.payment.PaymentProvider;
import com.eventra.payment.PaymentService;
import com.eventra.repository.*;
import com.eventra.seat.SeatLockService;
import com.eventra.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Development/demo data that depends on "now": accounts with BCrypt passwords from configuration,
 * a rolling window of shows, and a handful of real bookings made through the actual booking and
 * mock-payment pipeline (so seat maps and analytics are not empty). Never enabled in production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalTime[] SLOTS = {LocalTime.of(9, 45), LocalTime.of(13, 15), LocalTime.of(16, 45), LocalTime.of(20, 15)};

    private final AppProperties props;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PartnerRepository partnerRepository;
    private final CinemaRepository cinemaRepository;
    private final CinemaScreenRepository screenRepository;
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewService reviewService;
    private final SeatLockService seatLockService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager txm;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!props.seed().demoData()) return;
        TransactionTemplate tx = new TransactionTemplate(txm);
        tx.executeWithoutResult(s -> seedUsers());
        tx.executeWithoutResult(s -> seedReviews());
        int shows = ensureShows();
        tx.executeWithoutResult(s -> rollEvents());
        seedSampleBookings();
        log.info("Demo data ready ({} new shows). Log in as demo@eventra.com / admin@eventra.com / staff@eventra.com / partner@eventra.com", shows);
    }

    /** Keeps the rolling show window filled as days pass. */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Kolkata")
    public void nightly() {
        if (!props.seed().demoData()) return;
        ensureShows();
        new TransactionTemplate(txm).executeWithoutResult(s -> rollEvents());
    }

    private void seedUsers() {
        String password = props.seed().demoPassword();
        if (password == null || password.isBlank()) {
            log.warn("DEMO_USER_PASSWORD not set; demo accounts not created");
            return;
        }
        User demo = user("demo@eventra.com", "Demo Customer", "+91 90000 00001", password, "USER");
        User admin = user("admin@eventra.com", "Asha Admin", "+91 90000 00002", password, "ADMIN", "USER");
        user("staff@eventra.com", "Sanjay Staff", "+91 90000 00003", password, "VENUE_STAFF");
        User partnerUser = user("partner@eventra.com", "Priya Partner", "+91 90000 00004", password, "PARTNER");
        jdbc.update("insert into admin_users (user_id, title) values (?, 'Platform Admin') on conflict do nothing", admin.getId());
        if (demo.getPreferredCity() == null) {
            jdbc.update("update users set preferred_city_id = (select id from cities where slug = 'hyderabad') where id = ?", demo.getId());
        }
        if (partnerRepository.findByUserId(partnerUser.getId()).isEmpty()) {
            Partner p = new Partner();
            p.setName("Purple Square Entertainment");
            p.setContactEmail("partner@eventra.com");
            p.setUser(partnerUser);
            partnerRepository.save(p);
            jdbc.update("insert into cinema_partners (cinema_id, partner_id) select id, ? from cinemas where slug like 'purple-square-%' on conflict do nothing", p.getId());
        }
    }

    private User user(String email, String name, String phone, String password, String... roles) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFullName(name);
            u.setPhone(phone);
            u.setPasswordHash(passwordEncoder.encode(password));
            u.setEmailVerified(true);
            u.getRoles().addAll(roleRepository.findByNameIn(List.of(roles)));
            return userRepository.save(u);
        });
    }

    private static final String[] REVIEWERS = {"Kiran Reddy", "Meenakshi Iyer", "Rahul Verma", "Fatima Shaikh", "Arnav Das",
            "Lavanya Rao", "Vikash Kumar", "Sneha Pillai", "Harpreet Singh", "Deepa Nair", "Omar Khan", "Ritika Joshi"};
    private static final String[][] REVIEW_TEXT = {
            {"Worth every rupee", "Gorgeous visuals and a story that actually lands. Watched it in the big format and it was a proper event."},
            {"Solid entertainer", "A bit long in the second half, but the performances carry it. Great with friends."},
            {"Loved the music", "The background score is the real hero. Some scenes gave me goosebumps."},
            {"Good, not great", "Interesting idea, uneven execution. Still enjoyable on a weekend."},
            {"Instant favourite", "Funny, warm and surprisingly emotional. Will watch again."},
            {"Skip the 3D", "Story is fine but the 3D adds nothing. Go for 2D."}};

    private void seedReviews() {
        if (reviewRepository.count() > 0) return;
        List<User> reviewers = new ArrayList<>();
        for (int i = 0; i < REVIEWERS.length; i++) {
            String email = "reviewer" + (i + 1) + "@example.eventra.com";
            User u = userRepository.findByEmail(email).orElseGet(() -> {
                User x = new User();
                x.setEmail(email);
                x.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // not a usable login
                x.getRoles().addAll(roleRepository.findByNameIn(List.of("USER")));
                return x;
            });
            u.setFullName(REVIEWERS[i]);
            reviewers.add(userRepository.save(u));
        }
        Random rnd = new Random(42);
        for (Movie m : movieRepository.findByActiveTrueAndStatusOrderByReleaseDateAsc(MovieStatus.NOW_SHOWING)) {
            int base = 3 + (int) (m.getId() % 3); // deterministic "quality" per title
            for (User u : reviewers) {
                if (rnd.nextInt(10) < 3) continue;
                int rating = Math.max(1, Math.min(5, base + rnd.nextInt(3) - 1));
                String[] text = REVIEW_TEXT[rnd.nextInt(REVIEW_TEXT.length)];
                Review r = new Review();
                r.setUser(u);
                r.setMovieId(m.getId());
                r.setRating((short) rating);
                r.setTitle(text[0]);
                r.setBody(text[1]);
                r.setStatus(ReviewStatus.APPROVED);
                reviewRepository.save(r);
            }
            reviewRepository.flush();
            reviewService.recompute(m.getId());
        }
    }

    /** Creates four shows per active screen per day for the configured window, skipping days already scheduled. */
    public int ensureShows() {
        List<Movie> movies = movieRepository.findByActiveTrueAndStatusOrderByReleaseDateAsc(MovieStatus.NOW_SHOWING);
        if (movies.isEmpty()) return 0;
        LocalDate today = LocalDate.now(clock.withZone(IST));
        Instant now = Instant.now(clock);
        int created = 0;
        TransactionTemplate tx = new TransactionTemplate(txm);
        List<CinemaScreen> screens = screenRepository.findByActiveTrue();
        for (int si = 0; si < screens.size(); si++) {
            CinemaScreen screen = screens.get(si);
            for (int d = 0; d <= props.seed().showDaysAhead(); d++) {
                LocalDate date = today.plusDays(d);
                if (showRepository.existsByScreenIdAndShowDate(screen.getId(), date)) continue;
                final int screenIndex = si;
                final int day = d;
                Integer n = tx.execute(s -> createDay(screen, date, movies, screenIndex, day, now));
                created += n == null ? 0 : n;
            }
        }
        return created;
    }

    private int createDay(CinemaScreen screenRef, LocalDate date, List<Movie> movies, int screenIndex, int day, Instant now) {
        CinemaScreen screen = screenRepository.findById(screenRef.getId()).orElseThrow();
        Cinema cinema = screen.getCinema();
        String city = cinema.getCity().getSlug();
        int created = 0;
        for (int slot = 0; slot < SLOTS.length; slot++) {
            // re-load inside this transaction: the list was fetched outside it, so its lazy collections are unusable
            Movie movie = movieRepository.findById(movies.get((screenIndex * 3 + day + slot * 2) % movies.size()).getId()).orElseThrow();
            Instant start = date.atTime(SLOTS[slot]).atZone(IST).toInstant();
            if (!start.isAfter(now.plus(Duration.ofMinutes(20)))) continue;
            Instant end = start.plus(Duration.ofMinutes(movie.getDurationMin() + 20L));
            if (showRepository.existsOverlap(screen.getId(), start, end, null)) continue;
            String format = bestFormat(screen, movie);
            Show show = new Show();
            show.setMovie(movie);
            show.setCinema(cinema);
            show.setScreen(screen);
            show.setShowDate(date);
            show.setStartTime(start);
            show.setEndTime(end);
            show.setFormat(format);
            show.setLanguage(language(movie, city, slot));
            show.setBasePrice(price(format, SLOTS[slot], date));
            showRepository.saveAndFlush(show);
            showSeatRepository.generateForShow(show.getId(), screen.getId(), show.getBasePrice());
            created++;
        }
        return created;
    }

    private static String bestFormat(CinemaScreen screen, Movie movie) {
        List<String> sf = screen.formatList();
        List<String> mf = movie.formatList();
        for (String f : List.of("IMAX", "4DX", "DOLBY_ATMOS", "3D", "2D")) {
            if (sf.contains(f) && mf.contains(f) && !(f.equals("3D") && screen.getScreenType().equals("STANDARD") && movie.getId() % 2 == 0)) return f;
        }
        return "2D";
    }

    private static String language(Movie movie, String city, int slot) {
        List<String> langs = new ArrayList<>(movie.getLanguages());
        String regional = switch (city) {
            case "hyderabad", "vijayawada", "visakhapatnam" -> "Telugu";
            case "chennai" -> "Tamil";
            case "bengaluru" -> "Kannada";
            case "kolkata", "mumbai", "delhi", "pune" -> "Hindi";
            default -> "English";
        };
        if (slot % 2 == 0 && langs.contains(regional)) return regional;
        return langs.get(slot % langs.size());
    }

    private static BigDecimal price(String format, LocalTime time, LocalDate date) {
        int base = switch (format) {
            case "IMAX" -> 280;
            case "4DX" -> 320;
            case "3D", "DOLBY_ATMOS" -> 220;
            default -> 180;
        };
        if (time.getHour() >= 17) base += 40;
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) base += 30;
        return BigDecimal.valueOf(base);
    }

    /** Events whose sessions have all passed get a fresh session a few weeks out, with the same ticket tiers. */
    private void rollEvents() {
        Instant now = Instant.now(clock);
        for (Event e : eventRepository.findWithoutFutureSessions(now)) {
            EventSession last = e.getSessions().stream().max(Comparator.comparing(EventSession::getStartTime)).orElse(null);
            if (last == null) continue;
            long weeks = Math.max(1, Duration.between(last.getStartTime(), now).toDays() / 7 + 2);
            EventSession s = new EventSession();
            s.setEvent(e);
            s.setStartTime(last.getStartTime().plus(Duration.ofDays(7 * weeks)));
            s.setEndTime(last.getEndTime().plus(Duration.ofDays(7 * weeks)));
            for (EventTicketCategory c : last.getTicketCategories()) {
                EventTicketCategory n = new EventTicketCategory();
                n.setSession(s);
                n.setName(c.getName());
                n.setDescription(c.getDescription());
                n.setPrice(c.getPrice());
                n.setCapacity(c.getCapacity());
                n.setMaxPerBooking(c.getMaxPerBooking());
                n.setSortOrder(c.getSortOrder());
                s.getTicketCategories().add(n);
            }
            e.getSessions().add(s);
        }
    }

    /**
     * Makes real bookings for reviewer accounts through the same services customers use (seat lock ->
     * booking -> mock payment -> confirmation), so the seat maps show sold seats and analytics has data.
     */
    private void seedSampleBookings() {
        if (!props.payments().mockEnabled() || bookingRepository.count() > 0) return;
        List<User> buyers = userRepository.findAll().stream().filter(u -> u.getEmail().endsWith("@example.eventra.com")).toList();
        if (buyers.isEmpty()) return;
        Instant now = Instant.now(clock);
        List<Show> upcoming = showRepository.findAll().stream()
                .filter(s -> s.getStartTime().isAfter(now.plus(Duration.ofHours(1))) && s.getStartTime().isBefore(now.plus(Duration.ofDays(3))))
                .toList();
        Random rnd = new Random(7);
        int made = 0;
        for (Show show : upcoming) {
            if (rnd.nextInt(10) < 4) continue;
            List<String> rows = List.of("D", "E", "F", "G", "H");
            int groups = 1 + rnd.nextInt(4);
            for (int g = 0; g < groups; g++) {
                User buyer = buyers.get(rnd.nextInt(buyers.size()));
                String row = rows.get(rnd.nextInt(rows.size()));
                int startSeat = 1 + rnd.nextInt(10);
                int count = 2 + rnd.nextInt(3);
                List<String> seats = new ArrayList<>();
                for (int k = 0; k < count && startSeat + k <= 14; k++) seats.add(row + (startSeat + k));
                try {
                    var lock = seatLockService.lockSeats(show.getId(), buyer.getId(), seats);
                    var food = rnd.nextBoolean() ? List.of(new FoodLine(1L, 1), new FoodLine(6L, seats.size())) : List.<FoodLine>of();
                    var booking = bookingService.createFromLock(buyer.getId(), new CreateBookingRequest(lock.lockId(), food));
                    PaymentMethod method = List.of(PaymentMethod.UPI, PaymentMethod.CARD, PaymentMethod.GPAY, PaymentMethod.NETBANKING).get(rnd.nextInt(4));
                    var payment = paymentService.initiate(buyer.getId(), new InitiatePaymentRequest(booking.id(), method));
                    Long paymentId = paymentRepository.findByPublicId(payment.paymentId()).orElseThrow().getId();
                    paymentService.applyOutcome(paymentId, PaymentProvider.Outcome.SUCCESS, "seed_" + UUID.randomUUID().toString().substring(0, 8), null, "DEMO_SEED");
                    made++;
                } catch (RuntimeException e) {
                    log.debug("sample booking skipped: {}", e.getMessage());
                }
            }
        }
        log.info("Seeded {} sample bookings through the booking pipeline", made);
    }
}