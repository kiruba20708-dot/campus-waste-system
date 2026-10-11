

               

package com.campuswaste;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SpringBootApplication
@RestController
public class CampusWasteSystemApplication {

    private final WasteReportRepository reportRepository;

    private static final String STAFF_PASSWORD =
            System.getenv().getOrDefault(
                    "CAMPUS_STAFF_PASSWORD", "staff123");

    public CampusWasteSystemApplication(
            WasteReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(
                CampusWasteSystemApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        return """
                <!DOCTYPE html>
                <html><body style="font-family:Arial;text-align:center">
                <h1>Campus Waste Reporting System</h1>
                <p>Keep our campus clean and green!</p>
                <a href="/student">Student Portal</a>
                </body></html>
                """;
    }

    @GetMapping("/student")
    public String student() {
        return """
                <!DOCTYPE html>
                <html><body style="font-family:Arial;max-width:600px;margin:30px auto">
                <h1>Student Portal</h1>
                <h3>Submit a Waste Report</h3>
                <form action="/submit" method="post">
                <label>Your Name</label><br>
                <input name="name" required maxlength="100"><br><br>
                <label>Location</label><br>
                <input name="location" required maxlength="150"><br><br>
                <label>Waste Type</label><br>
                <select name="type">
                <option>Plastic</option>
                <option>Food Waste</option>
                <option>Paper</option>
                <option>Electronic Waste</option>
                <option>Other</option>
                </select><br><br>
                <label>Description</label><br>
                <textarea name="description" required maxlength="1000"></textarea><br><br>
                <button type="submit">Submit Report</button>
                </form>
                <hr>
                <a href="/my-reports">Track My Reports</a>
                </body></html>
                """;
    }

    @PostMapping("/submit")
    public String submit(
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam String type,
            @RequestParam String description) {

        List<String> allowedTypes = List.of(
                "Plastic", "Food Waste", "Paper",
                "Electronic Waste", "Other");

        if (name.isBlank() || location.isBlank()
                || description.isBlank()
                || !allowedTypes.contains(type)) {
            return """
                    <h2>Invalid report details.</h2>
                    <a href="/student">Try Again</a>
                    """;
        }

        WasteReport report = new WasteReport(
                name.trim(),
                location.trim(),
                type,
                description.trim());

        WasteReport saved = reportRepository.save(report);

        // Email notification using Resend API
        try {
            String apiKey = System.getenv("RESEND_API_KEY");
            String recipient = System.getenv("NOTIFICATION_EMAIL");

            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "RESEND_API_KEY is not configured");
            }

            if (recipient == null || recipient.isBlank()) {
                throw new IllegalStateException(
                        "NOTIFICATION_EMAIL is not configured");
            }

            Resend resend = new Resend(apiKey);

            CreateEmailOptions message =
                    CreateEmailOptions.builder()
                            .from("onboarding@resend.dev")
                            .to(recipient)
                            .subject("New Campus Waste Report Submitted")
                            .text(
                                    "A new waste report has been submitted.\n\n"
                                    + "Report ID: " + saved.getId() + "\n"
                                    + "Student Name: " + name.trim() + "\n"
                                    + "Location: " + location.trim() + "\n"
                                    + "Waste Type: " + type + "\n"
                                    + "Description: " + description.trim())
                            .build();

            resend.emails().send(message);
            System.out.println(
                    "Resend notification request completed.");

        } catch (Exception e) {
            System.err.println(
                    "Email notification failed: " + e.getMessage());
        }

        return """
                <!DOCTYPE html>
                <html><body style="font-family:Arial;text-align:center;margin:50px">
                <h1>Report Submitted Successfully!</h1>
                <p>Your report has been saved.</p>
                <p>Save your Report ID:</p>
                <h2>%s</h2>
                <p>Status: Pending</p>
                <a href="/my-reports">Track My Reports</a><br><br>
                <a href="/student">Submit Another Report</a>
                </body></html>
                """.formatted(escape(saved.getId().toString()));
    }

    @GetMapping("/my-reports")
    public String trackingPage() {
        return """
                <!DOCTYPE html>
                <html><body style="font-family:Arial">
                <h1>Track Your Report</h1>
                <form action="/track" method="get">
                <label>Report ID</label>
                <input name="id" type="number" min="1" required>
                <button type="submit">Check Status</button>
                </form>
                <a href="/student">Student Portal</a>
                </body></html>
                """;
    }

    @GetMapping("/track")
    public String track(@RequestParam String id) {
        try {
            Long reportId = Long.parseLong(id);

            if (reportId < 1) {
                return "<h2>Invalid Report ID.</h2>";
            }

            return reportRepository.findById(reportId)
                    .map(r -> """
                            <!DOCTYPE html>
                            <html><body style="font-family:Arial">
                            <h1>Report Status</h1>
                            <p>Report ID: %s</p>
                            <p>Status: %s</p>
                            <h3>Staff Reply</h3><p>%s</p>
                            <h3>Reason</h3><p>%s</p>
                            <a href="/my-reports">Back</a>
                            </body></html>
                            """.formatted(
                            escape(r.getId().toString()),
                            escape(r.getStatus()),
                            escape(r.getReply()),
                            escape(r.getReason())))
                    .orElse("<h2>Report ID not found.</h2>");

        } catch (NumberFormatException e) {
            return "<h2>Invalid Report ID.</h2>";
        }
    }

    @GetMapping("/staff")
    public String staffLogin() {
        return """
                <!DOCTYPE html>
                <html><body style="font-family:Arial">
                <h1>Staff Portal Login</h1>
                <form action="/staff/login" method="post">
                <input type="password" name="password" required>
                <button type="submit">Login</button>
                </form>
                </body></html>
                """;
    }

    @PostMapping("/staff/login")
    public String staffLoginCheck(
            @RequestParam String password,
            HttpSession session) {

        if (!STAFF_PASSWORD.equals(password)) {
            return """
                    <h2>Incorrect password</h2>
                    <a href="/staff">Try Again</a>
                    """;
        }

        session.setAttribute("staffLoggedIn", true);

        return """
                <!DOCTYPE html>
                <html><head>
                <meta http-equiv="refresh" content="0;url=/staff/dashboard">
                </head><body>Login successful.</body></html>
                """;
    }

    @GetMapping("/staff/dashboard")
    public String staffDashboard(HttpSession session) {
        if (!isStaffLoggedIn(session)) {
            return """
                    <h2>Please login first.</h2>
                    <a href="/staff">Staff Login</a>
                    """;
        }

        List<WasteReport> reports =
                reportRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "id"));

        StringBuilder page = new StringBuilder("""
                <!DOCTYPE html>
                <html><body style="font-family:Arial;margin:25px">
                <h1>Staff Dashboard</h1>
                <form action="/staff/logout" method="post">
                <button>Logout</button></form>
                """);

        if (reports.isEmpty()) {
            page.append("<p>No reports submitted yet.</p>");
        }

        for (WasteReport r : reports) {
            page.append("<hr><h2>Report ID: ")
                    .append(escape(r.getId().toString()))
                    .append("</h2><p>Name: ")
                    .append(escape(r.getName()))
                    .append("</p><p>Location: ")
                    .append(escape(r.getLocation()))
                    .append("</p><p>Type: ")
                    .append(escape(r.getType()))
                    .append("</p><p>Description: ")
                    .append(escape(r.getDescription()))
                    .append("</p><p>Status: ")
                    .append(escape(r.getStatus()))
                    .append("</p><p>Previous Reply: ")
                    .append(escape(r.getReply()))
                    .append("</p>")
                    .append("""
                            <form action="/staff/update" method="post">
                            <input type="hidden" name="id" value="%s">
                            <label>Status</label>
                            <select name="status">
                            <option>Pending</option>
                            <option>Accepted</option>
                            <option>In Progress</option>
                            <option>Rejected</option>
                            <option>Resolved</option>
                            </select><br><br>
                            <label>Reason</label><br>
                            <input name="reason" maxlength="500"><br><br>
                            <label>Reply to Student</label><br>
                            <textarea name="reply" maxlength="1000"></textarea><br><br>
                            <button type="submit">Save Update</button>
                            </form>
                            """.formatted(escape(r.getId().toString())));
        }

        page.append("</body></html>");
        return page.toString();
    }

    @PostMapping("/staff/update")
    public String update(
            @RequestParam Long id,
            @RequestParam String status,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam(defaultValue = "") String reply,
            HttpSession session) {

        if (!isStaffLoggedIn(session)) {
            return "<h2>Please login first.</h2><a href='/staff'>Login</a>";
        }

        List<String> allowed = List.of(
                "Pending", "Accepted", "In Progress",
                "Rejected", "Resolved");

        if (!allowed.contains(status)) {
            return "<h2>Invalid status.</h2>";
        }

        return reportRepository.findById(id)
                .map(report -> {
                    report.setStatus(status);
                    report.setReason(reason.trim());
                    report.setReply(reply.trim());
                    reportRepository.save(report);

                    return """
                            <h1>Update Saved Successfully!</h1>
                            <p>Report ID: %s</p>
                            <p>New Status: %s</p>
                            <a href="/staff/dashboard">Back to Dashboard</a>
                            """.formatted(
                            escape(id.toString()), escape(status));
                })
                .orElse("<h2>Report not found.</h2>");
    }

    @PostMapping("/staff/logout")
    public String staffLogout(HttpSession session) {
        session.invalidate();
        return """
                <!DOCTYPE html>
                <html><head>
                <meta http-equiv="refresh" content="0;url=/staff">
                </head><body>Logged out.</body></html>
                """;
    }

    private boolean isStaffLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(
                session.getAttribute("staffLoggedIn"));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
