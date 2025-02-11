CREATE TABLE `users` (
    `userId` CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    `username` VARCHAR(63) NOT NULL UNIQUE ,
    `password` VARCHAR(255) NOT NULL,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_userId` (`userId`),
    INDEX `idx_username` (`username`)
);

CREATE TABLE `boards` (
    `boardId` CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `games` (
    `gameId` CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    `gameCertificate` CHAR(36) NOT NULL UNIQUE,
    `boardId` CHAR(36) NOT NULL,
    `whiteUserId` CHAR(36) NOT NULL,
    `blackUserId` CHAR(36) NOT NULL,
    `winner` ENUM('WHITE', 'BLACK', 'DRAW') NULL,
    `gameState` ENUM('PENDING', 'ON_GOING', 'FINISHED') NOT NULL,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`boardId`) REFERENCES `boards`(`boardId`) ON DELETE CASCADE,
    FOREIGN KEY (`whiteUserId`) REFERENCES `users`(`userId`),
    FOREIGN KEY (`blackUserId`) REFERENCES `users`(`userId`),
    INDEX `idx_gameId` (`gameId`),
    INDEX `idx_gameCertificate` (`gameCertificate`),
    INDEX `idx_boardId` (`boardId`),
    INDEX `idx_whiteUserId` (`whiteUserId`),
    INDEX `idx_blackUserId` (`blackUserId`)
);

CREATE TABLE `invitations` (
    `invitationId` CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    `gameCertificate` CHAR(36) NOT NULL UNIQUE,
    `whitePlayerId` CHAR(36) NOT NULL,
    `blackPlayerId` CHAR(36) NOT NULL,
    `hasWhiteAccepted` BOOLEAN NOT NULL DEFAULT FALSE,
    `hasBlackAccepted` BOOLEAN NOT NULL DEFAULT FALSE,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_gameCertificate` (`gameCertificate`)
);

CREATE TABLE `boardHistories` (
    `boardHistoryId` CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    `boardId` CHAR(36) NOT NULL,
    `actionType` ENUM('MOVE', 'DROP', 'RESIGN') NOT NULL,
    `actionNumber` INT NOT NULL,
    `fromX` INT, -- not null for MOVE, null for DROP and RESIGN
    `fromY` INT,-- not null for MOVE, null for DROP and RESIGN
    `dropType` ENUM('ROOK', 'BISHOP', 'LANCE', 'KNIGHT', 'SILVER', 'GOLD', 'PAWN') NULL, -- not null for DROP, null for MOVE and RESIGN
    `toX` INT, -- not null for MOVE and DROP, null for RESIGN
    `toY` INT, -- not null for MOVE and DROP, null for RESIGN
    `toPromote` BOOLEAN NOT NULL,
    `player` ENUM('WHITE_PLAYER', 'BLACK_PLAYER') NOT NULL,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`boardId`) REFERENCES `boards`(`boardId`) ON DELETE CASCADE,
    INDEX `idx_boardId` (`boardId`),
    INDEX `idx_moveNumber` (`actionNumber`)
);