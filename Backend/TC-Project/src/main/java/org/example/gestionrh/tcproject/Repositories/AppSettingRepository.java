package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
