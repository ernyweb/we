This `database` folder is a local-development seed only.

Notes:
- The web game is a static client app and cannot write to files on the server when served by a static HTTP server.
- At runtime, the app stores data in the browser (localStorage). We've implemented obfuscation (hex(base64(JSON))) and password hashing to make casual inspection harder, but this does not make it secure against a determined attacker.
- For a truly secure and tamper-resistant leaderboard and user storage, run a local server with a backend that stores data on disk and enforces authentication.

Files here are examples and do not affect the running client unless you implement a server to expose them.
