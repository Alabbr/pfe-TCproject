import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProfileModalService {
  private openModalSubject = new Subject<void>();
  openModal$ = this.openModalSubject.asObservable();

  open() {
    this.openModalSubject.next();
  }
}
