import { test } from '../src/fixtures/test';

test('TC01 - Register User', async ({ homePage, signupLoginPage, signupPage, user }) => {
  await homePage.open();
  await homePage.header.goToSignupLogin();
  await signupLoginPage.expectSignupSectionVisible();

  await signupLoginPage.startSignup(user.name, user.email);
  await signupPage.expectAccountInfoVisible();
  await signupPage.fillAndSubmit(user);

  await signupPage.expectAccountCreated();
  await signupPage.continueAfterCreation();
  await homePage.header.expectLoggedInAs(user.name);

  await homePage.header.deleteAccount();
  await signupPage.expectAccountDeleted();
  await signupPage.continueAfterDeletion();
});
