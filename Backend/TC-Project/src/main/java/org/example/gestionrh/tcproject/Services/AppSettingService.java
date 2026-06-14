package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.AppSetting;
import org.example.gestionrh.tcproject.Repositories.AppSettingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppSettingService {

    private final AppSettingRepository repository;

    public AppSettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public String getSettingValue(String key) {
        Optional<AppSetting> setting = repository.findById(key);
        return setting.map(AppSetting::getSettingValue).orElse(null);
    }

    public AppSetting updateSetting(String key, String value) {
        AppSetting setting = repository.findById(key).orElse(new AppSetting(key, ""));
        setting.setSettingValue(value);
        return repository.save(setting);
    }
}
