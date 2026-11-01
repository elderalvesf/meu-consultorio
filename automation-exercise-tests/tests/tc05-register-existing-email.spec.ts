import { test } from '../src/fixtures/test';

test('TC05 - Register User with existing email', async ({
  homePage,
  signupLoginPage,
  registeredUser,
}) => {
  await homePage.open();
  await homePage.header.goToSignupLogin();
  await signupLoginPage.expectSignupSectionVisible();

  await signupLoginPage.startSignup(registeredUser.name, registeredUser.email);
  await signupLoginPage.expectEmailAlreadyExistsError();
});
