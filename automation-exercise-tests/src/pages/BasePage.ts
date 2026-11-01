import type { Page } from '@playwright/test';
import { HeaderComponent } from './HeaderComponent';
import { FooterComponent } from './FooterComponent';

export abstract class BasePage {
  readonly header: HeaderComponent;
  readonly footer: FooterComponent;

  constructor(protected readonly page: Page) {
    this.header = new HeaderComponent(page);
    this.footer = new FooterComponent(page);
  }
}
