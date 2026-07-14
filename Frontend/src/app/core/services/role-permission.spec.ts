import { TestBed } from '@angular/core/testing';

import { RolePermission } from './role-permission';

describe('RolePermission', () => {
  let service: RolePermission;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RolePermission);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
