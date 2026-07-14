import { TestBed } from '@angular/core/testing';

import { TransferRequest } from './transfer-request';

describe('TransferRequest', () => {
  let service: TransferRequest;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TransferRequest);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
