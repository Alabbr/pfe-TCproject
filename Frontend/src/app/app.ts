import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './shared/components/toast/toast';
import { ConfirmModalComponent } from './shared/components/confirm-modal/confirm-modal';
import { AiCopilotComponent } from './shared/components/ai-copilot/ai-copilot.component';

import { InactivityTimeoutService } from './core/services/inactivity-timeout.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent, ConfirmModalComponent, AiCopilotComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('tc-hub');

  constructor(private inactivityTimeoutService: InactivityTimeoutService) {
    // Service is injected and starts listening automatically
  }
}
