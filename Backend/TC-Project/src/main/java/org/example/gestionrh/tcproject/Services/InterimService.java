package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.InterimDelegation;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Entities.GlobalAnnouncement;
import org.example.gestionrh.tcproject.Repositories.InterimDelegationRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.example.gestionrh.tcproject.Repositories.GlobalAnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class InterimService {

    private final InterimDelegationRepository interimRepo;
    private final UserRepository userRepository;
    private final GlobalAnnouncementRepository announcementRepo;

    // Constructeur : injecte les repositories nécessaires (intérim, utilisateurs, annonces)
    public InterimService(InterimDelegationRepository interimRepo, UserRepository userRepository, GlobalAnnouncementRepository announcementRepo) {
        this.interimRepo = interimRepo;
        this.userRepository = userRepository;
        this.announcementRepo = announcementRepo;
    }

    // Assigne un intérimaire pour remplacer un chef : désactive les délégations précédentes et crée une annonce globale
    public InterimDelegation assignInterim(Long chefId, Long interimUserId, LocalDate startDate, LocalDate endDate) {
        User chef = userRepository.findById(chefId).orElseThrow(() -> new RuntimeException("Chef not found"));
        User interimUser = userRepository.findById(interimUserId).orElseThrow(() -> new RuntimeException("Interim user not found"));

        // Disable any currently active delegation for this chef
        List<InterimDelegation> activeDels = interimRepo.findActiveDelegationsForChef(chefId, LocalDate.now());
        for (InterimDelegation d : activeDels) {
            d.setActive(false);
            interimRepo.save(d);
        }

        InterimDelegation delegation = InterimDelegation.builder()
                .delegator(chef)
                .interim(interimUser)
                .startDate(startDate)
                .endDate(endDate)
                .isActive(true)
                .build();
        
        delegation = interimRepo.save(delegation);

        // Create global announcement
        String msg = String.format("La Direction Générale vous informe que %s remplacera %s en tant que Chef de %s du %s au %s.",
                interimUser.getFullName(), chef.getFullName(), chef.getDepartment() != null ? chef.getDepartment().getName() : "Non Spécifié", startDate.toString(), endDate.toString());
        
        GlobalAnnouncement announcement = GlobalAnnouncement.builder()
                .title("Avis d'Intérim : " + (chef.getDepartment() != null ? chef.getDepartment().getName() : ""))
                .message(msg)
                .isActive(true)
                .build();
        announcement = announcementRepo.save(announcement);

        delegation.setAnnouncementId(announcement.getId());
        interimRepo.save(delegation);

        return delegation;
    }

    // Retourne les délégations actives d'un chef (où il est le délégant)
    public List<InterimDelegation> getActiveDelegationsForChef(Long chefId) {
        return interimRepo.findActiveDelegationsForChef(chefId, LocalDate.now());
    }

    // Retourne les délégations actives d'un intérimaire (où il est le remplaçant)
    public List<InterimDelegation> getActiveDelegationsForInterim(Long interimUserId) {
        return interimRepo.findActiveDelegationsForInterim(interimUserId, LocalDate.now());
    }

    // Révoque une délégation d'intérim et désactive l'annonce associée
    public void revokeDelegation(Long delegationId) {
        InterimDelegation d = interimRepo.findById(delegationId).orElseThrow(() -> new RuntimeException("Delegation not found"));
        d.setActive(false);
        interimRepo.save(d);

        if (d.getAnnouncementId() != null) {
            announcementRepo.findById(d.getAnnouncementId()).ifPresent(a -> {
                a.setActive(false);
                announcementRepo.save(a);
            });
        }
    }
}
