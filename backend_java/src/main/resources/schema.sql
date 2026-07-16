SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `hashed_password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(20) DEFAULT 'member',
  `points` INT DEFAULT 0,
  `current_project_id` INT DEFAULT NULL
);

DROP TABLE IF EXISTS `projects`;
CREATE TABLE `projects` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `external_doc_url` VARCHAR(255),
  `invite_code` VARCHAR(50) UNIQUE
);

ALTER TABLE `users` ADD CONSTRAINT `fk_user_project` FOREIGN KEY (`current_project_id`) REFERENCES `projects`(`id`) ON DELETE SET NULL;

DROP TABLE IF EXISTS `project_members`;
CREATE TABLE `project_members` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `project_id` INT NOT NULL,
  `is_approved` BOOLEAN DEFAULT FALSE,
  `role` VARCHAR(50) DEFAULT 'member',
  `focus_minutes` INT DEFAULT 0,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
);

DROP TABLE IF EXISTS `project_member_badges`;
CREATE TABLE `project_member_badges` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `project_member_id` INT NOT NULL,
  `badge_type` VARCHAR(50),
  `earned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`project_member_id`) REFERENCES `project_members`(`id`) ON DELETE CASCADE
);

DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(150) NOT NULL,
  `description` TEXT,
  `status` VARCHAR(20) DEFAULT 'todo',
  `phase` VARCHAR(20) DEFAULT 'none',
  `weight` INT DEFAULT 1,
  `tags` VARCHAR(255),
  `commit_link` VARCHAR(255),
  `ai_review_score` INT DEFAULT NULL,
  `ai_review_comment` TEXT,
  `project_id` INT NOT NULL,
  `assigned_to_id` INT DEFAULT NULL,
  `priority` VARCHAR(20) DEFAULT 'medium',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deadline` DATETIME,
  `is_deleted` BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`assigned_to_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
);

DROP TABLE IF EXISTS `standups`;
CREATE TABLE `standups` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `project_id` INT NOT NULL,
  `date` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `yesterday` TEXT,
  `today` TEXT,
  `blockers` TEXT,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
);

DROP TABLE IF EXISTS `file_attachments`;
CREATE TABLE `file_attachments` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `task_id` INT NOT NULL,
  `uploader_id` INT NOT NULL,
  `filename` VARCHAR(255),
  `file_path` VARCHAR(255),
  `version` INT DEFAULT 1,
  `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `tasks`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`uploader_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
);

DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `message` VARCHAR(255),
  `type` VARCHAR(50),
  `is_read` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
);

DROP TABLE IF EXISTS `logs`;
CREATE TABLE `logs` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT,
  `project_id` INT,
  `action` VARCHAR(255),
  `target_type` VARCHAR(50),
  `target_id` INT,
  `details` TEXT,
  `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE SET NULL
);

DROP TABLE IF EXISTS `mall_items`;
CREATE TABLE `mall_items` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `project_id` INT NOT NULL,
  `name` VARCHAR(100),
  `description` VARCHAR(255),
  `price` INT,
  `icon` VARCHAR(50) DEFAULT '🎁',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
);

DROP TABLE IF EXISTS `purchase_history`;
CREATE TABLE `purchase_history` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `project_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `item_id` INT NOT NULL,
  `purchased_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`item_id`) REFERENCES `mall_items`(`id`) ON DELETE CASCADE
);

SET FOREIGN_KEY_CHECKS = 1;
