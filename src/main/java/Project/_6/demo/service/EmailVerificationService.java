package Project._6.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import Project._6.demo.entity.Concern;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailVerificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String toEmail, String firstName, String code) {
        try {
            String displayName = firstName == null || firstName.isBlank() ? "Student" : firstName.trim();

            String subject = "Your SCMS Email Verification Code";
            String html = """
                    <div style=\"font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;\">
                        <div style=\"background: #b91c1c; color: #ffffff; padding: 18px 24px;\">
                            <h2 style=\"margin: 0; font-size: 22px;\">Academy of Knowledge Bridge</h2>
                            <p style=\"margin: 6px 0 0 0; font-size: 13px; opacity: 0.95;\">Student Concern Management System</p>
                        </div>
                        <div style=\"padding: 22px 24px; color: #111827;\">
                            <img src=\"cid:brandLogo\" alt=\"Institute Logo\" style=\"width: 84px; height: 84px; object-fit: cover; border-radius: 10px; margin-bottom: 14px;\" />
                            <p style=\"margin: 0 0 12px 0;\">Hello %s,</p>
                            <p style=\"margin: 0 0 8px 0;\">Use this code to verify your email address for SCMS registration:</p>
                            <div style=\"display: inline-block; margin: 8px 0 14px 0; padding: 10px 16px; font-size: 26px; letter-spacing: 3px; font-weight: 800; color: #b91c1c; background: #fff1f2; border: 1px dashed #fda4af; border-radius: 8px;\">%s</div>
                            <p style=\"margin: 0 0 10px 0;\">This code expires in <strong>10 minutes</strong>.</p>
                            <p style=\"margin: 0 0 16px 0; color: #6b7280; font-size: 13px;\">If you did not request this, you can ignore this email.</p>
                            <p style=\"margin: 0;\">Regards,<br/><strong>Academy of Knowledge Bridge</strong></p>
                        </div>
                        <div style=\"background: #f9fafb; padding: 12px 24px; color: #6b7280; font-size: 12px;\">
                            This is an automated verification email from SCMS.
                        </div>
                    </div>
                    """.formatted(
                    escapeHtml(displayName),
                    escapeHtml(code)
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/images/img1.jpeg");
            if (logo.exists()) {
                helper.addInline("brandLogo", logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send verification code email.", ex);
        }
    }

    public void sendApprovalEmail(String toEmail, String firstName, String studentId) {
        sendStatusEmail(toEmail, firstName, studentId, true);
    }

    public void sendRejectionEmail(String toEmail, String firstName, String studentId) {
        sendStatusEmail(toEmail, firstName, studentId, false);
    }

    public void sendPendingReviewEmail(String toEmail, String firstName, String studentId) {
        try {
            String displayName = firstName == null || firstName.isBlank() ? "Student" : firstName.trim();
            String normalizedStudentId = studentId == null ? "N/A" : studentId;

            String subject = "SCMS Registration Received - Pending Review";
            String statusText = "Your account registration is pending institute review.";
            String detailsText = "We have received your registration details. Our team will review your submission and notify you by email once the decision is made.";

            String html = """
                    <div style=\"font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;\">
                        <div style=\"background: #b91c1c; color: #ffffff; padding: 18px 24px;\">
                            <h2 style=\"margin: 0; font-size: 22px;\">Academy of Knowledge Bridge</h2>
                            <p style=\"margin: 6px 0 0 0; font-size: 13px; opacity: 0.95;\">Student Concern Management System</p>
                        </div>
                        <div style=\"padding: 22px 24px; color: #111827;\">
                            <img src=\"cid:brandLogo\" alt=\"Institute Logo\" style=\"width: 84px; height: 84px; object-fit: cover; border-radius: 10px; margin-bottom: 14px;\" />
                            <p style=\"margin: 0 0 12px 0;\">Hello %s,</p>
                            <p style=\"margin: 0 0 12px 0; font-weight: 700; color: #b45309;\">%s</p>
                            <p style=\"margin: 0 0 12px 0;\">Student ID: <strong>%s</strong></p>
                            <p style=\"margin: 0 0 16px 0;\">%s</p>
                            <p style=\"margin: 0;\">Regards,<br/><strong>Academy of Knowledge Bridge</strong></p>
                        </div>
                        <div style=\"background: #f9fafb; padding: 12px 24px; color: #6b7280; font-size: 12px;\">
                            This is an automated notification from SCMS.
                        </div>
                    </div>
                    """.formatted(
                    escapeHtml(displayName),
                    escapeHtml(statusText),
                    escapeHtml(normalizedStudentId),
                    escapeHtml(detailsText)
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/images/img1.jpeg");
            if (logo.exists()) {
                helper.addInline("brandLogo", logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send pending review notification email.", ex);
        }
    }

    public void sendPasswordResetCode(String toEmail, String firstName, String code) {
        try {
            String displayName = firstName == null || firstName.isBlank() ? "Student" : firstName.trim();

            String subject = "SCMS Password Reset Verification Code";
            String html = """
                    <div style=\"font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;\">
                        <div style=\"background: #b91c1c; color: #ffffff; padding: 18px 24px;\">
                            <h2 style=\"margin: 0; font-size: 22px;\">Academy of Knowledge Bridge</h2>
                            <p style=\"margin: 6px 0 0 0; font-size: 13px; opacity: 0.95;\">Student Concern Management System</p>
                        </div>
                        <div style=\"padding: 22px 24px; color: #111827;\">
                            <img src=\"cid:brandLogo\" alt=\"Institute Logo\" style=\"width: 84px; height: 84px; object-fit: cover; border-radius: 10px; margin-bottom: 14px;\" />
                            <p style=\"margin: 0 0 12px 0;\">Hello %s,</p>
                            <p style=\"margin: 0 0 8px 0;\">Use this code to verify your password reset request:</p>
                            <div style=\"display: inline-block; margin: 8px 0 14px 0; padding: 10px 16px; font-size: 26px; letter-spacing: 3px; font-weight: 800; color: #b91c1c; background: #fff1f2; border: 1px dashed #fda4af; border-radius: 8px;\">%s</div>
                            <p style=\"margin: 0 0 10px 0;\">This code expires in <strong>10 minutes</strong>.</p>
                            <p style=\"margin: 0 0 16px 0; color: #6b7280; font-size: 13px;\">If you did not request this, you can ignore this email.</p>
                            <p style=\"margin: 0;\">Regards,<br/><strong>Academy of Knowledge Bridge</strong></p>
                        </div>
                        <div style=\"background: #f9fafb; padding: 12px 24px; color: #6b7280; font-size: 12px;\">
                            This is an automated password reset email from SCMS.
                        </div>
                    </div>
                    """.formatted(
                    escapeHtml(displayName),
                    escapeHtml(code)
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/images/img1.jpeg");
            if (logo.exists()) {
                helper.addInline("brandLogo", logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send password reset code email.", ex);
        }
    }

    public void sendAdminCreationVerificationCode(String toEmail, String code) {
        try {
            String subject = "Admin Account Verification Code - SCMS";
            String html = """
                    <div style=\"font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;\">
                        <div style=\"background: #b91c1c; color: #ffffff; padding: 18px 24px;\">
                            <h2 style=\"margin: 0; font-size: 22px;\">Academy of Knowledge Bridge</h2>
                            <p style=\"margin: 6px 0 0 0; font-size: 13px; opacity: 0.95;\">Student Concern Management System</p>
                        </div>
                        <div style=\"padding: 22px 24px; color: #111827;\">
                            <img src=\"cid:brandLogo\" alt=\"Institute Logo\" style=\"width: 84px; height: 84px; object-fit: cover; border-radius: 10px; margin-bottom: 14px;\" />
                            <p style=\"margin: 0 0 10px 0;\">Owner requested admin account creation for this email.</p>
                            <p style=\"margin: 0 0 8px 0;\">Use this code to verify admin email ownership:</p>
                            <div style=\"display: inline-block; margin: 8px 0 14px 0; padding: 10px 16px; font-size: 26px; letter-spacing: 3px; font-weight: 800; color: #b91c1c; background: #fff1f2; border: 1px dashed #fda4af; border-radius: 8px;\">%s</div>
                            <p style=\"margin: 0 0 10px 0;\">This code expires in <strong>10 minutes</strong>.</p>
                            <p style=\"margin: 0; color: #6b7280; font-size: 13px;\">If this request is unexpected, ignore this email.</p>
                        </div>
                        <div style=\"background: #f9fafb; padding: 12px 24px; color: #6b7280; font-size: 12px;\">
                            This is an automated admin account verification email from SCMS.
                        </div>
                    </div>
                    """.formatted(escapeHtml(code));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/images/img1.jpeg");
            if (logo.exists()) {
                helper.addInline("brandLogo", logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send admin verification code email.", ex);
        }
    }

    private void sendStatusEmail(String toEmail, String firstName, String studentId, boolean approved) {
        try {
            String displayName = firstName == null || firstName.isBlank() ? "Student" : firstName.trim();
            String normalizedStudentId = studentId == null ? "N/A" : studentId;

            String subject = approved
                    ? "SCMS Account Approved - Academy of Knowledge Bridge"
                    : "SCMS Account Update - Academy of Knowledge Bridge";

            String statusText = approved
                    ? "Your student account has been approved."
                    : "Your student account registration has been declined.";

            String detailsText = approved
                    ? "You can now log in to the Student Concern Management System using your registered email and password."
                    : "If you need more information, please contact the institute administration.";

            String html = """
                    <div style=\"font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;\">
                        <div style=\"background: #b91c1c; color: #ffffff; padding: 18px 24px;\">
                            <h2 style=\"margin: 0; font-size: 22px;\">Academy of Knowledge Bridge</h2>
                            <p style=\"margin: 6px 0 0 0; font-size: 13px; opacity: 0.95;\">Student Concern Management System</p>
                        </div>
                        <div style=\"padding: 22px 24px; color: #111827;\">
                            <img src=\"cid:brandLogo\" alt=\"Institute Logo\" style=\"width: 84px; height: 84px; object-fit: cover; border-radius: 10px; margin-bottom: 14px;\" />
                            <p style=\"margin: 0 0 12px 0;\">Hello %s,</p>
                            <p style=\"margin: 0 0 12px 0; font-weight: 700; color: %s;\">%s</p>
                            <p style=\"margin: 0 0 12px 0;\">Student ID: <strong>%s</strong></p>
                            <p style=\"margin: 0 0 16px 0;\">%s</p>
                            <p style=\"margin: 0;\">Regards,<br/><strong>Academy of Knowledge Bridge</strong></p>
                        </div>
                        <div style=\"background: #f9fafb; padding: 12px 24px; color: #6b7280; font-size: 12px;\">
                            This is an automated notification from SCMS.
                        </div>
                    </div>
                    """.formatted(
                    escapeHtml(displayName),
                    approved ? "#166534" : "#b91c1c",
                    escapeHtml(statusText),
                    escapeHtml(normalizedStudentId),
                    escapeHtml(detailsText)
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/images/img1.jpeg");
            if (logo.exists()) {
                helper.addInline("brandLogo", logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send status notification email.", ex);
        }
    }

    public void sendConcernStatusEmail(Concern concern, String actionType) {
        try {
            if (concern == null || concern.getStudent() == null || concern.getStudent().getUser() == null || concern.getStudent().getUser().getEmail() == null) {
                return;
            }

            String toEmail = concern.getStudent().getUser().getEmail();
            String firstName = concern.getStudent().getUser().getFirstName();
            String displayName = firstName == null || firstName.isBlank() ? "Student" : firstName.trim();

            String concernRef = "CON-" + concern.getConcernId();
            String subject = "SCMS Concern Status Update - " + concernRef;

            String statusText = "";
            String detailsText = "";
            String color = "#b91c1c";

            switch (actionType) {
                case "SUBMITTED":
                    subject = "Concern Successfully Submitted - " + concernRef;
                    statusText = "We have successfully received your concern.";
                    detailsText = "Your concern regarding \"" + concern.getSubject() + "\" has been logged and is currently in the queue for administrator review.";
                    color = "#2563eb"; // Blue
                    break;
                case "READ":
                    subject = "Concern Under Review - " + concernRef;
                    statusText = "An administrator has begun reviewing your concern.";
                    detailsText = "Your concern regarding \"" + concern.getSubject() + "\" has been read by our administration team. It is currently in progress.";
                    color = "#f59e0b"; // Amber
                    break;
                case "REPLIED":
                    subject = "Admin Reply Received - " + concernRef;
                    statusText = "An administrator has replied to your concern.";
                    detailsText = "Your concern regarding \"" + concern.getSubject() + "\" has received a response. Please log in to the Student Concern Management System to view the reply.";
                    color = "#16a34a"; // Green
                    break;
                case "DELETED":
                    subject = "Concern Removed by Administration - " + concernRef;
                    statusText = "Your concern has been removed by an administrator.";
                    detailsText = "Your concern regarding \"" + concern.getSubject() + "\" has been deleted by the administration team. If you need further help, please submit a new concern with updated details.";
                    color = "#b91c1c"; // Red
                    break;
                default:
                    return;
            }

            String html = """
                    <div style=\"font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;\">
                        <div style=\"background: #b91c1c; color: #ffffff; padding: 18px 24px;\">
                            <h2 style=\"margin: 0; font-size: 22px;\">Academy of Knowledge Bridge</h2>
                            <p style=\"margin: 6px 0 0 0; font-size: 13px; opacity: 0.95;\">Student Concern Management System</p>
                        </div>
                        <div style=\"padding: 22px 24px; color: #111827;\">
                            <img src=\"cid:brandLogo\" alt=\"Institute Logo\" style=\"width: 84px; height: 84px; object-fit: cover; border-radius: 10px; margin-bottom: 14px;\" />
                            <p style=\"margin: 0 0 12px 0;\">Hello %s,</p>
                            <p style=\"margin: 0 0 12px 0; font-weight: 700; color: %s;\">%s</p>
                            <p style=\"margin: 0 0 12px 0;\">Reference ID: <strong>%s</strong></p>
                            <p style=\"margin: 0 0 16px 0;\">%s</p>
                            <p style=\"margin: 0;\">Regards,<br/><strong>Academy of Knowledge Bridge Administration</strong></p>
                        </div>
                        <div style=\"background: #f9fafb; padding: 12px 24px; color: #6b7280; font-size: 12px;\">
                            This is an automated concern status notification from SCMS. Please do not reply directly to this email.
                        </div>
                    </div>
                    """.formatted(
                    escapeHtml(displayName),
                    color,
                    escapeHtml(statusText),
                    escapeHtml(concernRef),
                    escapeHtml(detailsText)
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/images/img1.jpeg");
            if (logo.exists()) {
                helper.addInline("brandLogo", logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("Failed to send concern status email: " + ex.getMessage());
        }
    }

    public void sendConcernDepartmentChangedEmail(Concern concern, String previousDepartment, String newDepartment) {
        try {
            if (concern == null || concern.getStudent() == null || concern.getStudent().getUser() == null || concern.getStudent().getUser().getEmail() == null) {
                return;
            }

            String toEmail = concern.getStudent().getUser().getEmail();
            String firstName = concern.getStudent().getUser().getFirstName();
            String displayName = firstName == null || firstName.isBlank() ? "Student" : firstName.trim();

            String oldDept = previousDepartment == null || previousDepartment.isBlank() ? "Not Assigned" : previousDepartment.trim();
            String newDept = newDepartment == null || newDepartment.isBlank() ? "Not Assigned" : newDepartment.trim();

            String concernRef = "CON-" + concern.getConcernId();
            String subject = "Concern Department Updated - " + concernRef;
            String statusText = "Your concern has been reassigned to another department.";
            String detailsText = "Your concern regarding \"" + concern.getSubject() + "\" has been moved from \"" + oldDept + "\" to \"" + newDept + "\" by the administration team.";

            String html = """
                    <div style=\"font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;\">
                        <div style=\"background: #b91c1c; color: #ffffff; padding: 18px 24px;\">
                            <h2 style=\"margin: 0; font-size: 22px;\">Academy of Knowledge Bridge</h2>
                            <p style=\"margin: 6px 0 0 0; font-size: 13px; opacity: 0.95;\">Student Concern Management System</p>
                        </div>
                        <div style=\"padding: 22px 24px; color: #111827;\">
                            <img src=\"cid:brandLogo\" alt=\"Institute Logo\" style=\"width: 84px; height: 84px; object-fit: cover; border-radius: 10px; margin-bottom: 14px;\" />
                            <p style=\"margin: 0 0 12px 0;\">Hello %s,</p>
                            <p style=\"margin: 0 0 12px 0; font-weight: 700; color: #2563eb;\">%s</p>
                            <p style=\"margin: 0 0 12px 0;\">Reference ID: <strong>%s</strong></p>
                            <p style=\"margin: 0 0 8px 0;\">Previous Department: <strong>%s</strong></p>
                            <p style=\"margin: 0 0 12px 0;\">New Department: <strong>%s</strong></p>
                            <p style=\"margin: 0 0 16px 0;\">%s</p>
                            <p style=\"margin: 0;\">Regards,<br/><strong>Academy of Knowledge Bridge Administration</strong></p>
                        </div>
                        <div style=\"background: #f9fafb; padding: 12px 24px; color: #6b7280; font-size: 12px;\">
                            This is an automated concern update notification from SCMS. Please do not reply directly to this email.
                        </div>
                    </div>
                    """.formatted(
                    escapeHtml(displayName),
                    escapeHtml(statusText),
                    escapeHtml(concernRef),
                    escapeHtml(oldDept),
                    escapeHtml(newDept),
                    escapeHtml(detailsText)
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/images/img1.jpeg");
            if (logo.exists()) {
                helper.addInline("brandLogo", logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("Failed to send concern department changed email: " + ex.getMessage());
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
