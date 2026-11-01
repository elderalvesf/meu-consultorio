import { test } from '../src/fixtures/test';

test('TC02 - Login User with correct email and password', async ({
  homePage,
  signupLoginPage,
  signupPage,
  registeredUser,
}) => {
  await homePage.open();
  await homePage.header.goToSignupLogin();
  await signupLoginPage.expectLoginSectionVisible();

  await signupLoginPage.login(registeredUser.email, registeredUser.password);
  await homePage.header.expectLoggedInAs(registeredUser.name);

  await homePage.header.deleteAccount();
  await signupPage.expectAccountDeleted();
});
