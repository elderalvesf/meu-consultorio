import { buildUser } from '../src/data/user';
import { test } from '../src/fixtures/test';

test('TC10 - Verify Subscription in home page', async ({ homePage }) => {
  await homePage.open();
  await homePage.footer.expectSubscriptionVisible();

  await homePage.footer.subscribe(buildUser().email);
  await homePage.footer.expectSubscribed();
});
