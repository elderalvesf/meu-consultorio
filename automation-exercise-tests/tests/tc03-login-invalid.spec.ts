import { test } from '../src/fixtures/test';

test('TC03 - Login User with incorrect email and password', async ({
  homePage,
  signupLoginPage,
  user,
}) => {
  await homePage.open();
  await homePage.header.goToSignupLogin();
  await signupLoginPage.expectLoginSectionVisible();

  await signupLoginPage.login(user.email, 'senha-incorreta');
  await signupLoginPage.expectLoginError();
});
