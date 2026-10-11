
package com.campuswaste;

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
            <html><head><title>Campus Waste System</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            </head>
            <body style="font-family:Arial;background:#eef5ee;text-align:center;padding:40px">
            <h1>Campus Waste Reporting System</h1>
            <p>Keep our campus clean and green!</p>
            <p><a href="/student">Student Portal</a></p>
            <p><a href="/staff">Staff Portal</a></p>
            </body></html>
            """;
    }

    @GetMapping("/student")
    public String student() {
        return """
            <!DOCTYPE html>
            <html><head><title>Student Portal</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            </head>
            <body style="font-family:Arial;max-width:600px;margin:30px auto;padding:20px">
            <h1>Student Portal</h1>
            <h3>Submit a Waste Report</h3>
            <form action="/submit" method="post">
            <label>Your Name</label><br>
            <input name="name" required maxlength="100" style="width:95%;padding:10px"><br><br>
            <label>Location</label><br>
            <input name="location" required maxlength="150" style="width:95%;padding:10px"><br><br>
            <label>Waste Type</label><br>
            <select name="type" required style="width:100%;padding:10px">
            <option>Plastic</option>
            <option>Food Waste</option>
            <option>Paper</option>
            <option>Electronic Waste</option>
            <option>Other</option>
            </select><br><br>
            <label>Description</label><br>
            <textarea name="description" required maxlength="1000"
            style="width:95%;height:100px;padding:10px"></textarea><br><br>
            <button style="padding:12px 22px">Submit Report</button>
            </form>
            <p><a href="/my-reports">Track My Reports</a></p>
            <p><a href="/">Home</a></p>
            </body></html>
            """;
    }

    @PostMapping("/submit")
    public String submit(@RequestParam String name,
                         @RequestParam String location,
                         @RequestParam String type,
                         @RequestParam String description) {

        List<String> allowedTypes = List.of(
                "Plastic", "Food Waste", "Paper",
                "Electronic Waste", "Other");

        if (name.isBlank() || location.isBlank()
                || description.isBlank()
                || !allowedTypes.contains(type)) {
            return "<h2>Invalid report details.</h2>"
                    + "<a href='/student'>Try Again</a>";
        }

        WasteReport report = new WasteReport(
                name.trim(),
                location.trim(),
                type,
                description.trim());

        WasteReport saved = reportRepository.save(report);

        return """
            <!DOCTYPE html>
            <html><body style="font-family:Arial;text-align:center;margin:50px">
            <h1>Report Submitted Successfully!</h1>
            <p>Your report has been saved in the database.</p>
            <p>Save your Report ID:</p>
            <p style="font-size:28px"><b>%s</b></p>
            <p>Status: Pending</p>
            <p><a href="/my-reports">Track Reports</a></p>
            <p><a href="/student">Submit Another Report</a></p>
            <p><a href="/">Home</a></p>
            </body></html>
            """.formatted(escape(saved.getId().toString()));
    }

    @GetMapping("/my-reports")
    public String trackingPage() {
        return """
            <!DOCTYPE html>
            <html><body style="font-family:Arial;max-width:500px;margin:30px auto;padding:20px">
            <h1>Track Your Report</h1>
            <form action="/track" method="get">
            <label>Report ID</label><br>
            <input name="id" type="number" min="1" required style="padding:10px;width:90%">
            <br><br><button>Check Status</button>
            </form>
            <p><a href="/student">Student Portal</a></p>
            <p><a href="/">Home</a></p>
            </body></html>
            """;
    }

    @GetMapping("/track")
    public String track(@RequestParam String id) {
        try {
            Long reportId = Long.parseLong(id);

            if (reportId < 1) {
                return "<h2>Invalid Report ID.</h2>"
                        + "<a href='/my-reports'>Try Again</a>";
            }

            return reportRepository.findById(reportId)
                    .map(r -> """
                        <html><body style="font-family:Arial;margin:40px">
                        <h1>Report Status</h1>
                        <p>Report ID: %s</p>
                        <p>Status: <b>%s</b></p>
                        <p>Staff Reply: %s</p>
                        <p>Reason: %s</p>
                        <a href="/my-reports">Back</a>
                        </body></html>
                        """.formatted(
                            escape(r.getId().toString()),
                            escape(r.getStatus()),
                            escape(r.getReply()),
                            escape(r.getReason())))
                    .orElse(
                        "<h2>Report ID not found.</h2>"
                        + "<a href='/my-reports'>Try Again</a>");

        } catch (NumberFormatException e) {
            return "<h2>Invalid Report ID.</h2>"
                    + "<a href='/my-reports'>Try Again</a>";
        }
    }

    @GetMapping("/staff")
    public String staffLogin() {
        return """
            <!DOCTYPE html>
            <html><body style="font-family:Arial;max-width:450px;margin:40px auto;padding:20px">
            <h1>Staff Portal Login</h1>
            <form action="/staff/login" method="post">
            <label>Staff Password</label><br>
            <input type="password" name="password" required style="padding:10px;width:90%">
            <br><br><button>Login</button>
            </form>
            <a href="/">Home</a>
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
            <html><head>
            <meta http-equiv="refresh" content="0;url=/staff/dashboard">
            </head><body>
            Login successful.
            <a href="/staff/dashboard">Open Dashboard</a>
            </body></html>
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
            <html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            </head><body style="font-family:Arial;margin:25px">
            <h1>Staff Dashboard</h1>
            <p>Review reports and update their status.</p>
            <form action="/staff/logout" method="post">
            <button>Logout</button>
            </form>
            """);

        if (reports.isEmpty()) {
            page.append("<p>No reports submitted yet.</p>");
        }

        for (WasteReport r : reports) {
            page.append("<hr><h2>Report ID: ")
                .append(escape(r.getId().toString()))
                .append("</h2>")
                .append("<p><b>Name:</b> ")
                .append(escape(r.getName())).append("</p>")
                .append("<p><b>Location:</b> ")
                .append(escape(r.getLocation())).append("</p>")
                .append("<p><b>Type:</b> ")
                .append(escape(r.getType())).append("</p>")
                .append("<p><b>Description:</b> ")
                .append(escape(r.getDescription())).append("</p>")
                .append("<p><b>Current Status:</b> ")
                .append(escape(r.getStatus())).append("</p>")
                .append("<form action='/staff/update' method='post'>")
                .append("<input type='hidden' name='id' value='")
                .append(escape(r.getId().toString()))
                .append("'>")
                .append("""
                    <label>Update Status</label><br>
                    <select name="status" style="padding:8px">
                    <option>Pending</option>
                    <option>Accepted</option>
                    <option>In Progress</option>
                    <option>Rejected</option>
                    <option>Resolved</option>
                    </select><br><br>
                    <label>Reason (for rejection, if needed)</label><br>
                    <input name="reason" maxlength="500" style="width:90%;padding:8px"><br><br>
                    <label>Reply to Student</label><br>
                    <textarea name="reply" maxlength="1000" style="width:90%;height:60px"></textarea><br><br>
                    <button>Save Update</button>
                    </form>
                    """);
        }

        page.append("<hr><p><a href='/'>Home</a></p></body></html>");
        return page.toString();
    }

    @PostMapping("/staff/update")
    public String update(@RequestParam Long id,
                         @RequestParam String status,
                         @RequestParam(defaultValue = "") String reason,
                         @RequestParam(defaultValue = "") String reply,
                         HttpSession session) {

        if (!isStaffLoggedIn(session)) {
            return """
                <h2>Please login first.</h2>
                <a href="/staff">Staff Login</a>
                """;
        }

        List<String> allowed = List.of(
                "Pending", "Accepted", "In Progress",
                "Rejected", "Resolved");

        if (!allowed.contains(status)) {
            return "<h2>Invalid status</h2>"
                    + "<a href='/staff/dashboard'>Back</a>";
        }

        return reportRepository.findById(id)
                .map(report -> {
                    report.setStatus(status);
                    report.setReason(reason);
                    report.setReply(reply);

                    reportRepository.save(report);

                    return """
                        <html><body style="font-family:Arial;margin:40px">
                        <h1>Update Saved!</h1>
                        <p>Report ID: %s</p>
                        <p>New Status: %s</p>
                        <p>Student can check the status using their Report ID.</p>
                        <p><a href="/staff/dashboard">Back to Staff Dashboard</a></p>
                        </body></html>
                        """.formatted(
                            escape(id.toString()),
                            escape(status));
                })
                .orElse(
                    "<h2>Report not found.</h2>"
                    + "<a href='/staff/dashboard'>Back</a>");
    }

    @PostMapping("/staff/logout")
    public String staffLogout(HttpSession session) {
        session.invalidate();

        return """
            <html><head>
            <meta http-equiv="refresh" content="0;url=/staff">
            </head><body>
            Logged out.
            <a href="/staff">Login again</a>
            </body></html>
            """;
    }

    private boolean isStaffLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(
                session.getAttribute("staffLoggedIn"));
    }

    private String escape(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}