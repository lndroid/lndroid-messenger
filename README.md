Lndroid.Messenger - instant messages over Lightning Network for Android
=======================================================================

Lndroid.Messenger is an instant messaging app that sends messages over Lightning Network. The app can connect to any Lightning wallet based on [Lndroid.Framework](https://github.com/lndroid/lndroid-framework/) using [Lndroid.Client](https://github.com/lndroid/lndroid-client/) library. It is meant to demonstrate the capabilities of the framework, and with great luck to become a real messaging app.

The custom records used to attach messages to lightning payments are the same as in [Whatsat](https://github.com/joostjager/whatsat), except for the message signature, which is not yet implemented. When full compatibility is implemented, you'd be able to exchange messages with Whatsat apps.

To see the video demonstration of the app, check [this video](https://www.youtube.com/watch?v=bF-1QxFTvHU). Description of what is happening on the video is [available here](https://github.com/lndroid/lndroid-wallet/#here-is-what-you-see-on-the-lndroid-demo-video).

# Roadmap

- [x] Basic messenger that actually sends messages :)
- [ ] Add message signatures for compatibility with Whatsat, the sign is also a proof that a pubkey sent specific message at specific time
- [ ] Notifications on new messages, clickable
- [ ] Notifications and 'acceptance' of new messages from unknown contacts
- [ ] Settings, like 'messages from new contacts below X sats go to spam'
- [ ] Avatars
- [ ] Contact info, hide contact, set our own avatar/comment, balance, etc
- [ ] Last message in the contact list
- [ ] New message counter in the contact list
- [ ] Scroll to start of new messages on Dialog open
- [ ] Message management: info, copy, delete, forward, etc
- [ ] Mainnet, after wallet mainnet release?
- [ ] Emoji? Collectible emoji like crypto-kittens?
- [ ] Attachments?

# TODO

...This readme is just an intro, expand it to properly cover what Lndroid does, and how to use it...

# Dependencies

1. Android Room
2. AutoValue
3. Guava
4. Gson
5. Lndroid.Client

# Important

The whole Lndroid project is at the very early stages of development. No guarantees are made about API stability, or the like. Do not use Lndroid code on Bitcoin mainnet, as you're very likely to lose your funds.

# License

MIT

# Author

Artur Brugeman, brugeman.artur@gmail.com