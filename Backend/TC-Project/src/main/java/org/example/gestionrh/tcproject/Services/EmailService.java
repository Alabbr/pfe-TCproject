package org.example.gestionrh.tcproject.Services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    // Envoie un email au chef de département pour l'informer qu'un nouvel employé est en attente d'affectation
    public void sendChefNotificationEmail(String toEmail, String chefName, String employeeName, String departmentName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Tunisie Clearing - Nouvel Employé à Affecter");

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>" +
                    "<div style='background-color: #0D2B5E; padding: 20px; text-align: center;'>" +
                    "<h2 style='color: #C9A84C; margin: 0;'>Tunisie Clearing Hub</h2>" +
                    "</div>" +
                    "<div style='padding: 20px; color: #333;'>" +
                    "<h3>Bonjour " + chefName + ",</h3>" +
                    "<p>Un nouvel employé (<strong>" + employeeName + "</strong>) a été ajouté à votre département <strong>" + departmentName + "</strong>.</p>" +
                    "<p>Il est actuellement <span style='color: #d9534f; font-weight: bold;'>en attente d'affectation</span>.</p>" +
                    "<p>Veuillez vous connecter à votre <strong>Espace Chef</strong> pour lui attribuer un poste de travail (Gestion).</p>" +
                    "<br>" +
                    "<a href='http://192.168.100.112/login' style='display: inline-block; background-color: #1A4A8A; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold;'>Accéder à mon espace</a>" +
                    "</div>" +
                    "<div style='background-color: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #777;'>" +
                    "&copy; 2024 Tunisie Clearing. Tous droits réservés." +
                    "</div>" +
                    "</div>";

            helper.setText(htmlContent, true);
            javaMailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
    // Envoie un email de bienvenue au nouvel utilisateur avec ses identifiants de connexion
    public void sendWelcomeEmail(String toEmail, String fullName, String loginEmail, String password) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Bienvenue sur Tunisie Clearing Hub !");

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>" +
                    "<div style='background: linear-gradient(135deg, #0D2B5E, #1A4A8A); padding: 30px; text-align: center;'>" +
                    "<h2 style='color: #C9A84C; margin: 0; font-size: 24px;'>🎉 Bienvenue !</h2>" +
                    "<p style='color: rgba(255,255,255,0.8); margin: 8px 0 0;'>Tunisie Clearing Hub</p>" +
                    "</div>" +
                    "<div style='padding: 30px; color: #333;'>" +
                    "<h3 style='color: #0D2B5E;'>Bonjour " + fullName + ",</h3>" +
                    "<p>Votre compte a été créé avec succès sur la plateforme <strong>Tunisie Clearing Hub</strong>.</p>" +
                    "<div style='background: #f8f9fa; border-left: 4px solid #C9A84C; padding: 16px; border-radius: 4px; margin: 20px 0;'>" +
                    "<p style='margin: 0 0 8px; font-weight: bold; color: #0D2B5E;'>🔑 Vos identifiants de connexion :</p>" +
                    "<p style='margin: 4px 0;'><strong>Email :</strong> " + loginEmail + "</p>" +
                    "<p style='margin: 4px 0;'><strong>Mot de passe :</strong> " + password + "</p>" +
                    "</div>" +
                    "<p style='color: #777; font-size: 13px;'>⚠️ Nous vous recommandons de changer votre mot de passe dès votre première connexion.</p>" +
                    "<br>" +
                    "<a href='http://192.168.100.112/login' style='display: inline-block; background-color: #1A4A8A; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold;'>Se connecter →</a>" +
                    "</div>" +
                    "<div style='background-color: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #777;'>" +
                    "&copy; 2024 Tunisie Clearing. Tous droits réservés." +
                    "</div>" +
                    "</div>";

            helper.setText(htmlContent, true);
            javaMailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Failed to send welcome email to " + toEmail + ": " + e.getMessage());
        }
    }
}
