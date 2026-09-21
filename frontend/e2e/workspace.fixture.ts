import { Page, WebSocketRoute } from '@playwright/test';

const date = '2026-09-20T09:00:00Z';
export const user = {
  id: 1,
  displayName: 'Amar Kosovac',
  username: 'amar',
  email: 'amar@example.test',
  presence: 'AWAY',
  customStatusText: 'Planning the next release',
  customStatusEmoji: '',
  avatarUrl: null,
  title: 'Engineer',
  createdAt: date,
};
export const people = [
  user,
  {
    ...user,
    id: 2,
    displayName: 'Maya Chen',
    username: 'maya',
    email: 'maya@example.test',
    presence: 'ONLINE',
  },
  { ...user, id: 3, displayName: 'Oliver James', username: 'oliver', email: 'oliver@example.test' },
];
export const workspaces = [
  {
    id: 1,
    name: 'Studio North',
    slug: 'studio-north',
    ownerId: 1,
    currentUserRole: 'OWNER',
    createdAt: date,
    updatedAt: date,
  },
  {
    id: 2,
    name: 'Product Lab',
    slug: 'product-lab',
    ownerId: 1,
    currentUserRole: 'ADMIN',
    createdAt: date,
    updatedAt: date,
  },
];
export const channels = [
  'general',
  'design',
  'engineering',
  'product-announcements-and-release-planning',
  'team-leads',
].map((name, index) => ({
  id: index + 1,
  workspaceId: 1,
  name,
  slug: name,
  description:
    index === 0
      ? 'A little of everything. Team updates, ideas, and good conversations.'
      : 'Share progress and keep the team connected.',
  privateChannel: index === 4,
  createdById: 1,
  createdAt: date,
  updatedAt: date,
  archivedAt: null,
}));
export const messages = [
  {
    id: 101,
    senderId: 2,
    senderDisplayName: 'Maya Chen',
    content:
      'Morning team! The updated designs are ready for review. I focused on making navigation feel a little calmer and more intentional.',
    replyCount: 2,
  },
  {
    id: 102,
    senderId: 1,
    senderDisplayName: 'Amar Kosovac',
    content:
      'These look great. I’ll take a closer look at the channel header and composer this afternoon.',
    replyCount: 0,
  },
  {
    id: 103,
    senderId: 3,
    senderDisplayName: 'Oliver James',
    content:
      'I added the implementation notes to the handoff. Let me know if anything needs more detail.',
    replyCount: 0,
  },
].map((message, index) => ({
  channelId: 1,
  senderEmail: 'team@example.test',
  createdAt: date,
  updatedAt: date,
  deletedAt: null,
  mentions: [],
  pinned: false,
  threadRootMessageId: null,
  reactions:
    index === 0
      ? [{ emoji: '👍', count: 2, userIds: [1, 3], users: ['Amar Kosovac', 'Oliver James'] }]
      : [],
  attachments: [],
  ...message,
}));
export const conversations = [
  {
    id: 10,
    type: 'DIRECT',
    displayName: 'Maya Chen',
    participants: people.slice(0, 2),
    unreadCount: 2,
  },
  { id: 11, type: 'GROUP', displayName: 'Launch planning', participants: people, unreadCount: 0 },
].map((item) => ({
  customName: null,
  lastMessage: null,
  createdAt: date,
  updatedAt: date,
  ...item,
}));

export async function mockWorkspace(
  page: Page,
  options: { empty?: boolean; role?: string; longNames?: boolean } = {},
) {
  const requests: { method: string; path: string; body: any }[] = [];
  const sentFrames: string[] = [];
  const sockets: { socket: WebSocketRoute; subscriptions: Map<string, string> }[] = [];
  await page.addInitScript(() => {
    localStorage.setItem(
      'slack_clone_access_token',
      `test.${btoa(JSON.stringify({ exp: 4102444800 }))}.test`,
    );
  });
  await page.routeWebSocket('**/ws', (socket) => {
    const subscriptions = new Map<string, string>();
    sockets.push({ socket, subscriptions });
    socket.onMessage((data) => {
      const frame = data.toString();
      sentFrames.push(frame);
      if (frame.startsWith('CONNECT')) socket.send('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0');
      if (frame.startsWith('SUBSCRIBE')) {
        const id = frame.match(/\nid:([^\n]+)/)?.[1];
        const destination = frame.match(/\ndestination:([^\n]+)/)?.[1];
        if (id && destination) subscriptions.set(destination, id);
      }
      if (frame.startsWith('UNSUBSCRIBE')) {
        const id = frame.match(/\nid:([^\n]+)/)?.[1];
        for (const [destination, value] of subscriptions)
          if (value === id) subscriptions.delete(destination);
      }
      if (frame.startsWith('DISCONNECT')) socket.close();
    });
  });
  await page.route('**/api/**', async (route) => {
    const req = route.request(),
      url = new URL(req.url()),
      path = url.pathname;
    const method = req.method();
    let body: any = null;
    try {
      body = req.postDataJSON();
    } catch {
      /* Multipart attachment body. */
    }
    requests.push({ method, path: path + url.search, body });
    let data: any = [];
    const workspaceList = options.empty
      ? []
      : workspaces.map((w) => ({
          ...w,
          currentUserRole: options.role || w.currentUserRole,
          name: options.longNames
            ? 'Studio North — International Product Engineering and Design Collaboration'
            : w.name,
        }));
    const conversation =
      conversations.find((c) => path.startsWith(`/api/conversations/${c.id}`)) || conversations[0];
    if (path === '/api/auth/me' || path === '/api/users/me') data = user;
    else if (path === '/api/workspaces') data = workspaceList;
    else if (/\/channels\/archived$/.test(path)) data = [{ ...channels[1], archivedAt: date }];
    else if (/\/channels$/.test(path)) data = channels;
    else if (/\/mentionable-users$/.test(path) || /\/members$/.test(path))
      data = people.map((p) => ({
        ...p,
        userId: p.id,
        role: p.id === 1 ? 'OWNER' : 'MEMBER',
        joinedAt: date,
      }));
    else if (/\/invitations$/.test(path)) data = [];
    else if (path === '/api/conversations') data = options.empty ? [] : conversations;
    else if (path.endsWith('/hidden')) data = [conversations[1]];
    else if (path.endsWith('/eligible-users')) data = people.slice(1);
    else if (path.endsWith('/participants'))
      data = people.map((p) => ({
        ...p,
        userId: p.id,
        role: p.id === 1 ? 'CREATOR' : 'MEMBER',
        joinedAt: date,
      }));
    else if (path.endsWith('/receipts'))
      data = [
        {
          messageId: 102,
          deliveredCount: 1,
          readCount: 1,
          totalRecipients: 1,
          recipients: [{ userId: 2, displayName: 'Maya Chen', deliveredAt: date, read: true }],
        },
      ];
    else if (/\/conversations\/\d+(\/read)?$/.test(path))
      data = { ...conversation, unreadCount: 0 };
    else if (path.endsWith('/pins'))
      data = [
        {
          message: messages[0],
          pinnedByUserId: 1,
          pinnedByDisplayName: user.displayName,
          pinnedAt: date,
        },
      ];
    else if (path.endsWith('/thread'))
      data = [
        {
          ...messages[2],
          conversationId: conversation.id,
          content: 'I’ll review the details and share feedback here.',
          threadRootMessageId: 101,
        },
      ];
    else if (path.endsWith('/replies'))
      data = {
        ...messages[1],
        conversationId: conversation.id,
        id: 150,
        content: body.content,
        threadRootMessageId: 101,
      };
    else if (path.endsWith('/context'))
      data = { targetMessageId: 101, threadRootMessageId: null, messages };
    else if (/\/messages$/.test(path)) {
      const list = messages.map((m) => ({ ...m, conversationId: conversation.id }));
      data =
        method === 'POST'
          ? { ...list[1], id: 120, content: body.content }
          : path.includes('/conversations/')
            ? { messages: list, nextBefore: url.searchParams.has('before') ? null : 101 }
            : messages;
    } else if (path.includes('/reactions')) data = { ...messages[0], reactions: [] };
    else if (path === '/api/notifications')
      data = [
        {
          id: 1,
          text: 'Maya mentioned you in #general',
          actorAvatarUrl: null,
          createdAt: date,
          readAt: null,
          type: 'MENTION',
          workspaceId: 1,
          channelId: 1,
          channelMessageId: 101,
          conversationId: null,
        },
      ];
    else if (path.endsWith('/unread-count') || path.endsWith('/read-all'))
      data = { unreadCount: path.endsWith('/read-all') ? 0 : 1 };
    else if (path === '/api/search')
      data = {
        query: url.searchParams.get('q'),
        page: Number(url.searchParams.get('page')),
        size: 20,
        hasNext: url.searchParams.get('page') === '0',
        results: [
          {
            id: 101 + Number(url.searchParams.get('page')),
            type: 'CHANNEL_MESSAGE',
            title: 'Design review',
            snippet: 'The updated designs are ready for review.',
            contextName: '#general',
            workspaceId: 1,
            channelId: 1,
            conversationId: null,
            timestamp: date,
            threadRootMessageId: null,
          },
        ],
      };
    await route.fulfill({ json: data });
  });
  return {
    requests,
    sentFrames,
    subscribed(destination: string) {
      return sockets.some((s) => s.subscriptions.has(destination));
    },
    publish(destination: string, payload: unknown) {
      let count = 0;
      for (const { socket, subscriptions } of sockets) {
        const id = subscriptions.get(destination);
        if (id) {
          socket.send(
            `MESSAGE\nsubscription:${id}\nmessage-id:test-${Date.now()}\ndestination:${destination}\n\n${JSON.stringify(payload)}\0`,
          );
          count++;
        }
      }
      return count;
    },
  };
}
