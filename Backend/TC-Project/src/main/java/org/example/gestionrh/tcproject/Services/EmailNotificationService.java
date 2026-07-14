package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.Task;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTaskAssignmentEmail(Task task) {
        if (task.getAssignee() == null || task.getAssignee().getEmail() == null) return;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(task.getAssignee().getEmail());
        message.setSubject("Nouvelle tâche assignée : " + task.getTitle());
        message.setText("Bonjour " + task.getAssignee().getFirstName() + ",\n\n" +
                "Vous avez été assigné(e) à une nouvelle tâche dans le projet : " + task.getProject().getTitle() + ".\n\n" +
                "Titre de la tâche : " + task.getTitle() + "\n" +
                "Priorité : " + task.getPriority() + "\n" +
                "Date limite : " + (task.getDeadline() != null ? task.getDeadline().toString() : "Non définie") + "\n\n" +
                "Veuillez consulter la plateforme pour plus de détails.\n\n" +
                "Cordialement,\n" +
                "L'équipe Tunisie Clearing");
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace(); // Log error but don't crash the request
        }
    }

    public void sendTaskValidationRequestEmail(Task task) {
        if (task.getReporter() == null || task.getReporter().getEmail() == null) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(task.getReporter().getEmail());
        message.setSubject("Validation requise pour la tâche : " + task.getTitle());
        message.setText("Bonjour " + task.getReporter().getFirstName() + ",\n\n" +
                "Le collaborateur " + (task.getAssignee() != null ? task.getAssignee().getFirstName() + " " + task.getAssignee().getLastName() : "Inconnu") + 
                " a terminé la tâche : \"" + task.getTitle() + "\" (Projet: " + task.getProject().getTitle() + ").\n\n" +
                "Cette tâche nécessite votre validation. Veuillez vous connecter à la plateforme pour vérifier son travail.\n\n" +
                "Cordialement,\n" +
                "L'équipe Tunisie Clearing");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
