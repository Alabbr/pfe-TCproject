import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmployeeTasks } from './employee-tasks';

describe('EmployeeTasks', () => {
  let component: EmployeeTasks;
  let fixture: ComponentFixture<EmployeeTasks>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeTasks]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EmployeeTasks);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
