import { expect, type Page } from '@playwright/test';
import type { User } from '../data/user';
import { BasePage } from './BasePage';

/** Formulário "Enter Account Information" + "Address Information". */
export class SignupPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  async expectAccountInfoVisible() {
    await expect(this.page.getByText('Enter Account Information')).toBeVisible();
  }

  async fillAndSubmit(user: User) {
    await this.page.locator(user.title === 'Mr' ? '#id_gender1' : '#id_gender2').check();
    await this.page.getByTestId('password').fill(user.password);
    await this.page.getByTestId('days').selectOption(user.birthDay);
    await this.page.getByTestId('months').selectOption({ label: user.birthMonth });
    await this.page.getByTestId('years').selectOption(user.birthYear);
    await this.page.locator('#newsletter').check();
    await this.page.locator('#optin').check();

    await this.page.getByTestId('first_name').fill(user.firstName);
    await this.page.getByTestId('last_name').fill(user.lastName);
    await this.page.getByTestId('company').fill(user.company);
    await this.page.getByTestId('address').fill(user.address);
    await this.page.getByTestId('address2').fill(user.address2);
    await this.page.getByTestId('country').selectOption(user.country);
    await this.page.getByTestId('state').fill(user.state);
    await this.page.getByTestId('city').fill(user.city);
    await this.page.getByTestId('zipcode').fill(user.zipcode);
    await this.page.getByTestId('mobile_number').fill(user.mobile);
    await this.page.getByTestId('create-account').click();
  }

  async expectAccountCreated() {
    await expect(this.page.getByTestId('account-created')).toHaveText('Account Created!');
  }
  async continueAfterCreation() {
    await this.page.getByTestId('continue-button').click();
  }
  async expectAccountDeleted() {
    await expect(this.page.getByTestId('account-deleted')).toHaveText('Account Deleted!');
  }
  async continueAfterDeletion() {
    await this.page.getByTestId('continue-button').click();
  }
}
