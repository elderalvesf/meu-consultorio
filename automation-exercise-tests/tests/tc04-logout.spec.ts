import { expect, test } from '../src/fixtures/test';

test('TC04 - Logout User', async ({ page, homePage, signupLoginPage, registeredUser }) => {
  await homePage.open();
  await homePage.header.goToSignupLogin();
  await signupLoginPage.expectLoginSectionVisible();

  await signupLoginPage.login(registeredUser.email, registeredUser.password);
  await homePage.header.expectLoggedInAs(registeredUser.name);

  await homePage.header.logout();
  await expect(page).toHaveURL(/\/login/);
  await signupLoginPage.expectLoginSectionVisible();
  await homePage.header.expectLoggedOut();
});
