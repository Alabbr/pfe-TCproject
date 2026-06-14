import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
}

export interface ConfirmConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDanger?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  public toasts$ = this.toastsSubject.asObservable();
  private counter = 0;

  // Confirm Modal state
  private confirmStateSubject = new BehaviorSubject<{ config: ConfirmConfig | null; resolver: ((value: boolean) => void) | null }>({ config: null, resolver: null });
  public confirmState$ = this.confirmStateSubject.asObservable();

  constructor() {}

  show(type: 'success' | 'error' | 'warning' | 'info', title: string, message: string, duration = 4000) {
    const id = ++this.counter;
    const toast: ToastMessage = { id, type, title, message, duration };
    const currentToasts = this.toastsSubject.value;
    
    this.toastsSubject.next([...currentToasts, toast]);

    if (duration > 0) {
      setTimeout(() => this.remove(id), duration);
    }
  }

  success(title: string, message: string) { this.show('success', title, message); }
  error(title: string, message: string) { this.show('error', title, message, 6000); }
  warning(title: string, message: string) { this.show('warning', title, message); }
  info(title: string, message: string) { this.show('info', title, message); }

  remove(id: number) {
    const currentToasts = this.toastsSubject.value;
    this.toastsSubject.next(currentToasts.filter(t => t.id !== id));
  }

  confirm(config: ConfirmConfig): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.confirmStateSubject.next({ config, resolver: resolve });
    });
  }

  closeConfirm(result: boolean) {
    const { resolver } = this.confirmStateSubject.value;
    if (resolver) {
      resolver(result);
    }
    this.confirmStateSubject.next({ config: null, resolver: null });
  }
}
