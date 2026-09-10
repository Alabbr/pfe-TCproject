import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { NgxEchartsModule, provideEchartsCore } from 'ngx-echarts';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';
// @ts-ignore
import html2pdf from 'html2pdf.js';

@Component({
  selector: 'app-billing-dashboard',
  standalone: true,
  imports: [CommonModule, NgxEchartsModule],
  providers: [
    provideEchartsCore({ echarts }),
  ],
  templateUrl: './billing-dashboard.html',
  styleUrl: './billing-dashboard.scss'
})
export class BillingDashboardComponent implements OnInit {
  stats: any = null;
  loading = true;
  error = false;
  loadingPdf = false;

  // Prediction variables
  prediction: any = null;
  loadingPrediction = true;
  errorPrediction = false;
  predictionChartOption: EChartsOption = {};

  // Chart options
  tcnChartOption: EChartsOption = {};
  repoChartOption: EChartsOption = {};
  caPieOption: EChartsOption = {};

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.fetchData();
  }

  fetchData() {
    this.loading = true;
    this.http.get('/api/billing/dashboard-stats').subscribe({
      next: (data: any) => {
        this.stats = data;
        this.loading = false;
        this.initCharts();
      },
      error: (err) => {
        console.error('Error fetching billing stats', err);
        this.error = true;
        this.loading = false;
      }
    });

    this.loadingPrediction = true;
    this.http.get('/api/billing/prediction').subscribe({
      next: (data: any) => {
        this.prediction = data;
        this.loadingPrediction = false;
        this.initPredictionChart();
      },
      error: (err) => {
        console.error('Error fetching billing predictions', err);
        this.errorPrediction = true;
        this.loadingPrediction = false;
      }
    });
  }

  downloadPDF() {
    const element = document.getElementById('pdfContent');
    if (!element) {
      console.error('Élément pdfContent introuvable');
      return;
    }

    this.loadingPdf = true;

    const opt = {
      margin:       [10, 10, 10, 10], // Marges en mm
      filename:     `Facturation_TC_Hub_${new Date().toISOString().split('T')[0]}.pdf`,
      image:        { type: 'jpeg', quality: 0.98 },
      html2canvas:  { scale: 2, useCORS: true, logging: false },
      jsPDF:        { unit: 'mm', format: 'a3', orientation: 'portrait' }
    };

    // html2pdf est parfois importé différemment selon la config ts, on s'assure de l'appeler correctement
    const pdfGenerator = typeof html2pdf === 'function' ? html2pdf() : (html2pdf as any).default();

    pdfGenerator.set(opt).from(element).save().then(() => {
      this.loadingPdf = false;
    }).catch((err: any) => {
      console.error('Erreur lors de la génération du PDF:', err);
      this.loadingPdf = false;
    });
  }

  initCharts() {
    if (!this.stats) return;

    // TCN Chart
    this.tcnChartOption = this.createBarChartOption(this.stats.tcnData.chartData, 'TCN');
    
    // REPO Chart
    this.repoChartOption = this.createBarChartOption(this.stats.repoEspeceData.chartData, 'REPO');

    // Chiffre Affaires Pie Chart
    this.caPieOption = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c}%'
      },
      legend: {
        orient: 'horizontal',
        bottom: 0,
        textStyle: { color: '#64748b' }
      },
      color: ['#3b82f6', '#10b981', '#f59e0b'],
      series: [
        {
          name: 'Répartition',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '18',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: this.stats.chiffreAffaires.repartitionPie
        }
      ]
    };
  }

  private createBarChartOption(chartData: any[], title: string): EChartsOption {
    const labels = chartData.map(d => d.label).reverse();
    const factures = chartData.map(d => d.facture).reverse();
    const generes = chartData.map(d => d.genere).reverse();

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      legend: {
        data: ['Facturé', 'Généré'],
        bottom: 0,
        textStyle: { color: '#64748b' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '10%',
        top: '10%',
        containLabel: true
      },
      xAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
        axisLabel: { color: '#64748b' }
      },
      yAxis: {
        type: 'category',
        data: labels,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b' }
      },
      color: ['#10b981', '#f59e0b'],
      series: [
        {
          name: 'Facturé',
          type: 'bar',
          stack: 'total',
          emphasis: { focus: 'series' },
          data: factures,
          itemStyle: { borderRadius: [0, 4, 4, 0] }
        },
        {
          name: 'Généré',
          type: 'bar',
          stack: 'total',
          emphasis: { focus: 'series' },
          data: generes,
          itemStyle: { borderRadius: [0, 4, 4, 0] }
        }
      ]
    };
  }

  initPredictionChart() {
    if (!this.prediction || !this.prediction.historicalPoints) return;

    const points = this.prediction.historicalPoints;
    const years = points.map((p: any) => p.year.toString());
    
    const totalData = points.map((p: any) => p.total);
    const stockData = points.map((p: any) => p.stock);
    const fluxData = points.map((p: any) => p.flux);
    const ponctuelData = points.map((p: any) => p.ponctuel);

    // Filter prediction index for drawing visual marks
    const predIdx = points.findIndex((p: any) => p.isPrediction);

    this.predictionChartOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'line' }
      },
      legend: {
        data: ['CA Global (Machine Learning)', 'Stock', 'Flux', 'Ponctuel'],
        bottom: 0,
        textStyle: { color: '#64748b' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '18%',
        top: '10%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: years,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b' }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
        axisLabel: { color: '#64748b' }
      },
      color: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'],
      series: [
        {
          name: 'CA Global (Machine Learning)',
          type: 'line',
          data: totalData,
          smooth: true,
          symbolSize: 8,
          lineStyle: { width: 3 },
          markPoint: {
            data: predIdx !== -1 ? [
              {
                name: 'Prédiction',
                coord: [years[predIdx], totalData[predIdx]],
                value: 'Prédiction',
                itemStyle: { color: '#ef4444' }
              }
            ] : []
          }
        },
        {
          name: 'Stock',
          type: 'line',
          data: stockData,
          smooth: true,
          lineStyle: { type: 'dashed' }
        },
        {
          name: 'Flux',
          type: 'line',
          data: fluxData,
          smooth: true,
          lineStyle: { type: 'dashed' }
        },
        {
          name: 'Ponctuel',
          type: 'line',
          data: ponctuelData,
          smooth: true,
          lineStyle: { type: 'dashed' }
        }
      ]
    };
  }
}
