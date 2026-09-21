import { test, expect } from '@playwright/test';
import { mockWorkspace, messages } from './workspace.fixture';

const browserErrors = new WeakMap<object, string[]>();
test.beforeEach(async ({ page }) => {
  const errors: string[] = [];
  browserErrors.set(page, errors);
  page.on('pageerror', (error) => errors.push(error.message));
});
test.afterEach(async ({ page }) => {
  expect(browserErrors.get(page)).toEqual([]);
});

test('workspace navigation, account data, and empty state remain accessible', async ({ page }) => {
  await mockWorkspace(page);
  await page.goto('/workspaces');
  await expect(page.getByRole('heading', { name: 'Welcome to Studio North' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Settings', exact: true })).toBeHidden();
  await page.locator('.workspace-tools summary').click();
  await page.getByRole('button', { name: 'Settings', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Workspace settings' })).toBeVisible();
  await page.getByRole('button', { name: 'Close settings' }).click();
  await page.getByRole('button', { name: 'Open account menu' }).click();
  await expect(page.locator('.account-menu')).toContainText('Away');
  await page.keyboard.press('Escape');
  await expect(page.getByRole('button', { name: 'Open account menu' })).toBeFocused();
  await page.screenshot({ path: 'test-results/workspace-empty-1440.png' });
});

for (const width of [1920, 1440, 1366, 1024, 768, 390]) {
  test(`channel layout fits ${width}px with long names`, async ({ page }) => {
    await page.setViewportSize({ width, height: width === 390 ? 844 : 900 });
    await mockWorkspace(page, { longNames: true });
    await page.goto('/workspaces/1/channels/1');
    await expect(page.locator('.message-row')).toHaveCount(3);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
    expect(overflow).toBe(false);
    const composer = await page.locator('.message-composer').boundingBox();
    expect(composer!.x).toBeGreaterThanOrEqual(0);
    expect(composer!.x + composer!.width).toBeLessThanOrEqual(width);
    expect(composer!.y + composer!.height).toBeLessThanOrEqual(width === 390 ? 844 : 900);
    if (width <= 900) {
      await page.getByRole('button', { name: 'Open navigation' }).click();
      await expect(page.getByRole('button', { name: 'Open account menu' })).toBeVisible();
      await page.getByRole('button', { name: 'design', exact: true }).click();
      await expect(page.locator('.channel-sidebar')).toBeHidden();
    }
    await page.screenshot({ path: `test-results/channel-${width}.png` });
  });
}

test('notification and search popups retain readable text and working controls', async ({
  page,
}) => {
  const api = await mockWorkspace(page);
  await page.goto('/workspaces');
  await page.getByRole('button', { name: 'Notifications', exact: true }).click();
  const panel = page.locator('.notification-panel');
  await expect(panel).toContainText('Maya mentioned you');
  await expect(panel).toHaveCSS('color', 'rgb(31, 41, 55)');
  await page.getByRole('button', { name: 'Mark all read' }).click();
  await expect
    .poll(() => api.requests.some((r) => r.path.endsWith('/read-all') && r.method === 'POST'))
    .toBe(true);
  await page.getByRole('button', { name: 'Notifications', exact: true }).click();
  await page.getByRole('searchbox', { name: 'Search', exact: true }).fill('design');
  await expect(page.locator('.search-results')).toContainText('Design review');
  await page.getByRole('button', { name: 'People', exact: true }).click();
  await expect.poll(() => api.requests.some((r) => r.path.includes('type=PEOPLE'))).toBe(true);
  await page.getByRole('button', { name: 'Load more', exact: true }).click();
  await expect(page.locator('.search-results strong')).toHaveCount(2);
  await page.locator('.search-results button').filter({ hasText: 'Design review' }).first().click();
  await expect(page).toHaveURL(/targetMessage=101/);
  await expect(page.locator('#message-101')).toHaveClass(/search-highlight/);
});

test('channel composer, mentions, pins, and threads keep their integrations', async ({ page }) => {
  const api = await mockWorkspace(page);
  await page.goto('/workspaces/1/channels/1');
  await page.getByRole('textbox', { name: 'Channel message' }).fill('@ma');
  await expect(page.locator('.mention-autocomplete')).toContainText('Maya Chen');
  await page.getByRole('textbox', { name: 'Channel message' }).press('Enter');
  await expect(page.getByRole('textbox', { name: 'Channel message' })).toHaveValue('@maya ');
  await page.getByRole('button', { name: 'Pinned messages', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Pinned messages' })).toBeVisible();
  await page.locator('.modal-backdrop').click({ position: { x: 5, y: 5 } });
  await page
    .locator('#message-101')
    .getByRole('button', { name: /Thread/ })
    .click();
  await expect(page.getByRole('complementary', { name: 'Message thread' })).toBeVisible();
  await page.getByRole('textbox', { name: 'Reply in thread' }).fill('Thanks for the update!');
  await page.getByRole('button', { name: 'Reply', exact: true }).click();
  await expect
    .poll(() =>
      api.requests.some(
        (r) => r.path.endsWith('/replies') && r.body?.content === 'Thanks for the update!',
      ),
    )
    .toBe(true);
  await page.getByRole('button', { name: 'Close thread' }).click();
});

test('DM and group navigation preserve history, receipts, and thread rendering', async ({
  page,
}) => {
  const api = await mockWorkspace(page);
  await page.goto('/conversations/10');
  await expect(page.locator('.message-row')).toHaveCount(3);
  await expect(page.locator('.receipt-label')).toBeVisible();
  await page.getByRole('button', { name: 'Load older messages' }).click();
  await expect.poll(() => api.requests.some((r) => r.path.includes('before=101'))).toBe(true);
  await page
    .locator('#message-101')
    .getByRole('button', { name: /Thread/ })
    .click();
  await expect(page.getByRole('complementary', { name: 'Message thread' })).toBeVisible();
  await page.getByRole('button', { name: 'Close thread' }).click();
  await page.getByRole('button', { name: 'Launch planning', exact: true }).click();
  await page.getByRole('button', { name: 'Conversation settings' }).click();
  await page.getByRole('button', { name: 'Rename group' }).click();
  await expect(page.getByRole('heading', { name: 'Rename group' })).toBeVisible();
});

test('users without workspaces can still create a workspace and open invitations', async ({
  page,
}) => {
  await mockWorkspace(page, { empty: true });
  await page.goto('/workspaces');
  await expect(page.getByRole('heading', { name: 'Make room for your team' })).toBeVisible();
  await page.getByRole('button', { name: 'Create workspace', exact: true }).last().click();
  await expect(page.getByRole('dialog', { name: 'Create workspace' })).toBeVisible();
});

test('keyboard focus stays in dialogs and returns to the invoking button', async ({ page }) => {
  await mockWorkspace(page);
  await page.goto('/workspaces');
  const trigger = page.getByRole('button', { name: 'Create channel', exact: true }).first();
  await trigger.click();
  const dialog = page.getByRole('dialog', { name: 'Create channel' });
  await expect(dialog).toBeVisible();
  const close = dialog.getByRole('button', { name: 'Close create channel modal' });
  await expect(close).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(dialog.getByRole('button', { name: 'Create channel', exact: true })).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(close).toBeFocused();
  await close.click();
  await expect(trigger).toBeFocused();
  await page.getByRole('button', { name: 'Notifications', exact: true }).click();
  await page.getByRole('button', { name: 'Close notifications' }).click();
  await expect(page.getByRole('button', { name: 'Notifications', exact: true })).toBeFocused();
  await page.getByRole('searchbox', { name: 'Search', exact: true }).fill('design');
  await expect(page.locator('.search-results')).toBeVisible();
  await page.getByRole('button', { name: 'People', exact: true }).focus();
  await page.keyboard.press('Escape');
  await expect(page.locator('.search-results')).toBeHidden();
  await expect(page.getByRole('searchbox', { name: 'Search', exact: true })).toBeFocused();
});

test('workspace tools and private channel dialogs remain reachable', async ({ page }) => {
  await mockWorkspace(page);
  await page.goto('/workspaces');
  await page.locator('.workspace-tools summary').click();
  for (const [button, heading, close] of [
    ['People', 'Workspace members', 'Close members'],
    ['Manage invitations', 'Workspace invitations', 'Close invitation management'],
    ['Archived channels', 'Archived channels', 'Close archived channels'],
    ['Hidden conversations', 'Hidden conversations', 'Close hidden conversations'],
  ]) {
    await page.getByRole('button', { name: button, exact: true }).click();
    const dialog = page.getByRole('dialog').last();
    await expect(dialog).toContainText(heading);
    const closeButton = dialog.getByRole('button', { name: new RegExp(close, 'i') });
    if (await closeButton.count()) await closeButton.click();
    else await dialog.getByRole('button', { name: /Close/i }).first().click();
  }
  await page.getByRole('button', { name: 'Private channel: team-leads', exact: true }).click();
  await page.getByRole('button', { name: 'Manage channel members', exact: true }).click();
  await expect(page.locator('.channel-members-modal')).toBeVisible();
  await page.getByRole('button', { name: 'Close manage members modal' }).click();
  await page.getByRole('button', { name: 'Channel settings', exact: true }).click();
  await expect(page.locator('.channel-settings-modal')).toBeVisible();
});

test('regular members retain messaging access without administration actions', async ({ page }) => {
  await mockWorkspace(page, { role: 'MEMBER' });
  await page.goto('/workspaces');
  await page.locator('.workspace-tools summary').click();
  await expect(page.getByRole('button', { name: 'Manage invitations', exact: true })).toHaveCount(
    0,
  );
  await expect(page.getByRole('button', { name: 'Archived channels', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Settings', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'general', exact: true }).click();
  await expect(page.getByRole('textbox', { name: 'Channel message' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Channel settings', exact: true })).toHaveCount(0);
});

test('channel send, uploads, reactions, and incoming STOMP messages still work', async ({
  page,
}) => {
  const api = await mockWorkspace(page);
  await page.goto('/workspaces/1/channels/1');
  const destination = '/topic/workspaces/1/channels/1/messages';
  await expect.poll(() => api.subscribed(destination)).toBe(true);
  api.publish(destination, {
    type: 'MESSAGE_CREATED',
    message: { ...messages[0], id: 180, content: 'A live update from the team.' },
    threadRootMessageId: null,
  });
  await expect(page.locator('#message-180')).toContainText('A live update from the team.');
  api.publish('/topic/workspaces/1/channels/1/typing', {
    userId: 2,
    displayName: 'Maya Chen',
    typing: true,
  });
  await expect(page.locator('.typing-indicator')).toContainText('Maya Chen');
  await page
    .getByRole('textbox', { name: 'Channel message' })
    .fill('The channel composer still sends messages.');
  await page.getByRole('button', { name: 'Send', exact: true }).click();
  await expect
    .poll(() =>
      api.requests.some(
        (r) =>
          r.method === 'POST' && r.body?.content === 'The channel composer still sends messages.',
      ),
    )
    .toBe(true);
  await expect(page.getByRole('textbox', { name: 'Channel message' })).toHaveValue('');
  await page
    .getByLabel('Attach a file', { exact: true })
    .setInputFiles({
      name: 'review.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('Review notes'),
    });
  await expect(page.locator('.selected-file')).toHaveText('review.txt');
  await page.getByRole('textbox', { name: 'Channel message' }).fill('Here are the notes.');
  await page.getByRole('button', { name: 'Send', exact: true }).click();
  await expect
    .poll(() =>
      api.requests.some((r) => r.path === '/api/attachments/channel/120' && r.method === 'POST'),
    )
    .toBe(true);
  await page.locator('#message-101 .message-extras button').first().click();
  await expect
    .poll(() => api.requests.some((r) => r.path.includes('/reactions') && r.method === 'DELETE'))
    .toBe(true);
});

test('DM composer sends over STOMP and incoming updates reach the message view', async ({
  page,
}) => {
  const api = await mockWorkspace(page);
  await page.goto('/conversations/10');
  const destination = '/topic/users/1/conversations/10/messages';
  await expect.poll(() => api.subscribed(destination)).toBe(true);
  await page
    .getByRole('textbox', { name: 'Direct message', exact: true })
    .fill('Let’s catch up after the review.');
  await page.getByRole('button', { name: 'Send', exact: true }).click();
  await expect
    .poll(() =>
      api.sentFrames.some(
        (f) =>
          f.startsWith('SEND') &&
          f.includes('/app/conversations/10/messages') &&
          f.includes('Let’s catch up'),
      ),
    )
    .toBe(true);
  api.publish(destination, {
    type: 'MESSAGE_CREATED',
    message: { ...messages[0], conversationId: 10, id: 190, content: 'Sounds good, see you then!' },
    threadRootMessageId: null,
  });
  await expect(page.locator('#message-190')).toContainText('Sounds good, see you then!');
});

test('profile, status, invitations, and new-message dialogs fit a phone viewport', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockWorkspace(page);
  await page.goto('/workspaces');
  await page.getByRole('button', { name: 'Open navigation' }).click();
  await page.getByRole('button', { name: 'Open account menu' }).click();
  await page.getByRole('button', { name: 'Profile & settings', exact: true }).click();
  const profile = page.getByRole('dialog', { name: 'Profile & settings' });
  await expect(profile.getByLabel('Display name')).toHaveValue('Amar Kosovac');
  const box = await profile.boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(390);
  expect(box!.y + box!.height).toBeLessThanOrEqual(844);
  await page.getByRole('button', { name: 'Close profile settings' }).click();
  await page.getByRole('button', { name: 'Open account menu' }).click();
  await page.getByRole('button', { name: 'Set a status', exact: true }).click();
  await expect(
    page.getByRole('dialog', { name: 'Set a status' }).getByLabel('Status', { exact: true }),
  ).toHaveValue('Planning the next release');
  await page.getByRole('button', { name: 'Close status settings' }).click();
  await page.getByRole('button', { name: 'Start a new direct or group message' }).first().click();
  await expect(page.getByRole('dialog', { name: 'New message' })).toContainText('Maya Chen');
  await page.getByRole('button', { name: 'Close new message' }).click();
  await page.getByRole('button', { name: 'Close navigation', exact: true }).last().click();
  await page.getByRole('button', { name: 'Pending workspace invitations' }).click();
  await expect(page.getByRole('dialog', { name: 'Workspace invitations' })).toBeVisible();
  await page.getByRole('button', { name: 'Close pending invitations' }).click();
});


test('switching conversations clears the previous thread panel and draft', async ({ page }) => {
  await mockWorkspace(page);
  await page.goto('/workspaces/1/channels/1');
  await page.locator('#message-101').getByRole('button', { name: /Thread/ }).click();
  await page.getByRole('textbox', { name: 'Reply in thread' }).fill('A draft for this channel only');
  await page.getByRole('button', { name: 'Maya Chen 2', exact: true }).click();
  await expect(page.getByRole('complementary', { name: 'Message thread' })).toHaveCount(0);
  await page.locator('#message-101').getByRole('button', { name: /Thread/ }).click();
  await expect(page.getByRole('textbox', { name: 'Reply in thread' })).toHaveValue('');
  await page.getByRole('button', { name: 'design', exact: true }).click();
  await expect(page.getByRole('complementary', { name: 'Message thread' })).toHaveCount(0);
});
