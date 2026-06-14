import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KpiCard } from '../../../shared/components/kpi-card/kpi-card';
import { NgxEchartsDirective } from 'ngx-echarts';
import { EChartsOption } from 'echarts';
import * as echarts from 'echarts/core';
import { PieChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/auth.model';
import { ChefDashboard } from '../chef-dashboard/chef-dashboard';
import { EmployeDashboard } from '../employe-dashboard/employe-dashboard';
import { SuperAdminDashboardService, SuperAdminStats } from '../../../core/services/super-admin-dashboard.service';

echarts.use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer]);

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, KpiCard, NgxEchartsDirective, ChefDashboard, EmployeDashboard],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit {
  currentUser: AuthResponse | null = null;
  stats: SuperAdminStats | null = null;

  deptChartOptions: EChartsOption = {};
  roleChartOptions: EChartsOption = {};

  constructor(
    private authService: AuthService,
    private adminStatsService: SuperAdminDashboardService
  ) {}

  ngOnInit() {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (this.isSuperAdmin()) {
        this.loadSuperAdminStats();
      }
    });
  }

  loadSuperAdminStats() {
    this.adminStatsService.getStats().subscribe(res => {
      this.stats = res;
      this.initCharts(res);
    });
  }

  initCharts(data: SuperAdminStats) {
    this.deptChartOptions = {
      tooltip: { trigger: 'item' },
      legend: { top: '5%', left: 'center', textStyle: { color: 'var(--color-text-main)' } },
      series: [
        {
          name: 'Département',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: 'var(--color-bg-white)',
            borderWidth: 2
          },
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: 20, fontWeight: 'bold', color: 'var(--color-text-main)' }
          },
          labelLine: { show: false },
          data: data.departmentDistribution,
          color: ['#0D2B5E', '#1A4A8A', '#C9A84C', '#6B7A90', '#10B981']
        }
      ]
    };

    this.roleChartOptions = {
      tooltip: { trigger: 'item' },
      legend: { top: 'bottom', textStyle: { color: 'var(--color-text-main)' } },
      series: [
        {
          name: 'Rôle',
          type: 'pie',
          radius: [20, 100],
          center: ['50%', '50%'],
          roseType: 'area',
          itemStyle: { borderRadius: 8 },
          data: data.roleDistribution,
          color: ['#C9A84C', '#1A4A8A', '#0D2B5E', '#10B981']
        }
      ]
    };
  }

  isSuperAdmin(): boolean {
    return this.currentUser?.role === 'SUPER_ADMIN' || this.currentUser?.role === 'DIRECTEUR_GENERAL';
  }

  isChef(): boolean {
    return this.currentUser?.role === 'DIRECTEUR' || this.currentUser?.role === 'RESPONSABLE';
  }

  isEmploye(): boolean {
    return this.currentUser?.role === 'EMPLOYE' || this.currentUser?.role === 'CONSULTANT_EXTERNE';
  }
}
