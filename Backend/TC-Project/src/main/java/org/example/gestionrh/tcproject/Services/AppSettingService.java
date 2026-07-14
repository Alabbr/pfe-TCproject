package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.AppSetting;
import org.example.gestionrh.tcproject.Repositories.AppSettingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppSettingService {

    private final AppSettingRepository repository;

    // Constructeur : injecte le repository des paramètres
    public AppSettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    // Récupère la valeur d'un paramètre par sa clé (retourne null si non trouvé)
    public String getSettingValue(String key) {
        Optional<AppSetting> setting = repository.findById(key);
        return setting.map(AppSetting::getSettingValue).orElse(null);
    }

    // Met à jour un paramètre existant ou en crée un nouveau si la clé n'existe pas
    public AppSetting updateSetting(String key, String value) {
        AppSetting setting = repository.findById(key).orElse(new AppSetting(key, ""));
        setting.setSettingValue(value);
        return repository.save(setting);
    }
}
