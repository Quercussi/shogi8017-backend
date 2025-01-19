CREATE TABLE `users` (
    `userId` CHAR(36) NOT NULL PRIMARY KEY,
    `username` VARCHAR(63) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_userId` (`userId`)
);

CREATE TABLE `boards` (
    `boardId` CHAR(36) NOT NULL PRIMARY KEY,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `games` (
    `gameId` CHAR(36) NOT NULL PRIMARY KEY,
    `boardId` CHAR(36) NOT NULL,
    `whiteUserId` CHAR(36),
    `blackUserId` CHAR(36),
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`boardId`) REFERENCES `boards`(`boardId`) ON DELETE CASCADE,
    FOREIGN KEY (`whiteUserId`) REFERENCES `users`(`userId`) ON DELETE SET NULL,
    FOREIGN KEY (`blackUserId`) REFERENCES `users`(`userId`) ON DELETE SET NULL,
    INDEX `idx_gameId` (`gameId`),
    INDEX `idx_boardId` (`boardId`),
    INDEX `idx_whiteUserId` (`whiteUserId`),
    INDEX `idx_blackUserId` (`blackUserId`)
);

CREATE TABLE `boardHistories` (
    `boardHistoryId` CHAR(36) NOT NULL PRIMARY KEY,
    `boardId` CHAR(36) NOT NULL,
    `moveNumber` INT NOT NULL,
    `moveFromX` INT NOT NULL,
    `moveFromY` INT NOT NULL,
    `moveToX` INT NOT NULL,
    `moveToY` INT NOT NULL,
    `promoteTo` ENUM('QUEEN', 'ROOK', 'BISHOP', 'KNIGHT'),
    `player` ENUM('WHITE', 'BLACK') NOT NULL,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`boardId`) REFERENCES `boards`(`boardId`) ON DELETE CASCADE,
    INDEX `idx_boardId` (`boardId`),
    INDEX `idx_moveNumber` (`moveNumber`)
);

CREATE TABLE `boardStateCaches` (
    `boardStateCacheId` CHAR(36) NOT NULL PRIMARY KEY,
    `boardId` CHAR(36) NOT NULL,
    `piece` ENUM('KING', 'QUEEN', 'ROOK', 'BISHOP', 'KNIGHT', 'PAWN') NOT NULL,
    `playerTurn` ENUM('WHITE', 'BLACK') NOT NULL,
    `posX` INT NOT NULL,
    `posY` INT NOT NULL,
    `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`boardId`) REFERENCES `boards`(`boardId`) ON DELETE CASCADE,
    INDEX `idx_boardId` (`boardId`)
);