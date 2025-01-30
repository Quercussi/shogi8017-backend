# shogi8017

### Requirements
- A person can create and edit an account (a user).
- A User can join / start a game.
    - User can choose the game duration (Blitz, Normal, Custom).
    - Random: User will be automatically matched with another user.
    - Invitation: User will invite another user.
        - User can accept/reject the invitation.
        - Invitation expires after a certain timeout.
- User can view previous games of any users.
- Users can interact during the game (move pieces, offer/reject draws, etc.).
- Users receive real-time updates about game events.
- Users can be notified of game events like game end, draw offer, etc.

---

### Pages
- **Main Page**
    - Join a random game
    - Invite a game
    - View account
    - View previous games
    - Search users
- **Account Page**
    - Edit username
    - Edit password
- **Search User Page**
    - Search users
    - View user history
- **Game Page**
    - Game board
    - Game history
    - User
    - Notifications
- **Game History Page**
    - Game history (with pagination or filtering)
    - Ranking information

---

### APIs

#### **General REST APIs**
These APIs handle one-time, stateless operations that don’t require real-time communication and are used before the game starts.

1. **Account Management**
    - `POST /api/accounts`
        - Create a new account.
        - **Request Body:** `{ "username": "string", "email": "string", "password": "string" }`
        - **Response:** `{ "accountId": "string", "username": "string" }`

    - `PUT /api/accounts/{accountId}`
        - Edit an existing account.
        - **Request Body:** `{ "username": "string?", "email": "string?", "password": "string?" }`
        - **Response:** `{ "accountId": "string", "username": "string", "email": "string" }`

2. **Game Management**
    - `POST /api/games/join`
        - Join a random game or create a new room.
        - **Request Body:** `{ "duration": "Blitz|Normal|Custom" }`
        - **Response:** `{ "roomId": "string" }`

    - `POST /api/games/invite`
        - Send an invitation to another user.
        - **Request Body:** `{ "invitingUserId": "string", "invitedUserId": "string" }`
        - **Response:** `{ "roomId": "string", "status": "Pending", "expiresAt": "timestamp" }`  # Added expiration for invitation

    - `POST /api/games/respond`
        - Respond to a game invitation (Accept/Reject).
        - **Request Body:** `{ "roomId": "string", "response": "Accept|Reject" }`
        - **Response:** `{ "roomId": "string", "status": "Accepted|Rejected" }`

3. **Game History**
    - `GET /api/games/history/{userId}`
        - Retrieve previous games of a user with pagination.
        - **Response:**
          ```json
          [
            {
              "gameId": "string",
              "players": ["string", "string"],
              "result": "Win|Loss|Draw",
              "duration": "string",
              "endTime": "timestamp"
            }
          ]
          ```

---

#### **Game REST APIs**
These APIs handle stateless game-related operations, which **require a roomId** since they pertain to a specific game.

1. **Room Details**
    - `GET /api/games/{roomId}`
        - Retrieve details of an active game within a specific room.
        - **Response:**
          ```json
          {
            "roomId": "string",
            "players": ["string", "string"],
            "currentState": "string",
            "duration": "Blitz|Normal|Custom"
          }
          ```

---

#### **Game WebSocket APIs**
Real-time actions during a game session. **Requires roomId for all requests after the game starts.**

1. **Connection Management**
    - `/ws/connect/{roomId}`
        - Establish a WebSocket connection for a specific room.
        - **Request:** `{ "roomId": "string" }`
        - **Response:** `{ "status": "Connected", "roomId": "string" }`

2. **Invitation Management**
    - `/ws/await_invitation/{roomId}`
        - Listen for invitations in real-time within the context of a room.
        - **Response:** `{ "invitingPlayer": "string", "duration": "Blitz|Normal|Custom" }`

    - `/ws/respond_invitation/{roomId}`
        - Respond to a game invitation in real-time for a specific room.
        - **Response:** `{ "roomId": "string", "status": "Accepted|Rejected" }`

---

#### **Gameplay Actions**
These are the real-time game actions that occur after the game has started and require a `roomId`.

1. **Gameplay Actions**
    - `/ws/move_piece/{roomId}`
        - Send a move in the game for the specific room.
        - **Request:** `{ "move": "e2e4" }`
        - **Response:** `{ "status": "Valid|Invalid", "updatedBoard": "string" }`

    - `/ws/resign/{roomId}`
        - Resign from the game in the specific room.
        - **Request:** `{ "reason": "string" }`
        - **Response:** `{ "success": true }`

2. **Draw Management**
    - `/ws/offer_draw/{roomId}`
        - Offer a draw to the opponent in the specific room.
        - **Request:** `{}`

    - `/ws/respond_draw/{roomId}`
        - Respond to a draw offer in the specific room.
        - **Request:** `{ "response": "Accept|Reject" }`

    - `/ws/await_draw_offer/{roomId}`
        - Listen for a draw offer in real-time in the specific room.
        - **Response:** `{ "offeredBy": "string" }`

3. **Game End Events**
    - `/ws/await_game_end/{roomId}`
        - Listen for the end of the game in the specific room.
        - **Response:** `{ "result": "Win|Loss|Draw", "winner": "string" }`

---

### Notes:
1. **General APIs** (like account creation, game invites) do not require `roomId` since they are part of the **pre-game** flow.
2. **Game-related APIs** (like game moves, offers, and responses) require a `roomId` because they are tied to specific game sessions.
3. **WebSocket connections** and interactions are scoped by `roomId`, ensuring that gameplay happens within the correct context.

---

### TODO:
1. Design Promotion API