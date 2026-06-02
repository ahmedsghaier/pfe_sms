import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MessageTemplateListComponent } from './list.component';

describe('ListComponent', () => {
  let component: MessageTemplateListComponent;
  let fixture: ComponentFixture<MessageTemplateListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MessageTemplateListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MessageTemplateListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
