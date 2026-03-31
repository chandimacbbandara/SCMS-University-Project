-- Microsoft SQL Server Schema Script for SCMS Application
-- Auto-generated from Spring Boot Entities

-- 1. Create User table (Base table for Student and Admin)
CREATE TABLE [User] (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Email VARCHAR(255) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Gender VARCHAR(10),
    Phone_number VARCHAR(15),
    First_Name VARCHAR(100),
    Last_Name VARCHAR(100),
    Address_1st_Lane VARCHAR(255),
    Address_2nd_Lane VARCHAR(255),
    Address_3rd_Lane VARCHAR(255),
    Registration_Status VARCHAR(20)
);

-- 2. Create Student table (Inherits from User)
CREATE TABLE Student (
    UserID INT PRIMARY KEY FOREIGN KEY REFERENCES [User](UserID),
    StudentID VARCHAR(20) NOT NULL UNIQUE,
    DOB DATE,
    StudentDPhoto VARBINARY(MAX),
    Category VARCHAR(50)
);

-- 3. Create Admin table (Inherits from User)
CREATE TABLE Admin (
    UserID INT PRIMARY KEY FOREIGN KEY REFERENCES [User](UserID),
    StaffID VARCHAR(20) NOT NULL UNIQUE
);

-- 4. Create Concern table
CREATE TABLE Concern (
    ConcernID INT IDENTITY(1,1) PRIMARY KEY,
    Subject VARCHAR(255) NOT NULL,
    Message VARCHAR(MAX) NOT NULL,
    Evidence VARCHAR(500),
    Category VARCHAR(100),
    AI_Priority_Level VARCHAR(20),
    Status VARCHAR(50),
    CreatedTime DATETIME2,
    StudentID_FK INT FOREIGN KEY REFERENCES Student(UserID),
    AdminID_FK INT FOREIGN KEY REFERENCES Admin(UserID)
);

-- 5. Create Admin_reply table
CREATE TABLE Admin_reply (
    ReplyID INT IDENTITY(1,1) PRIMARY KEY,
    Reply_Message VARCHAR(MAX) NOT NULL,
    Resolution_Screenshot VARCHAR(500),
    Reply_Time DATETIME2,
    AdminID_FK INT FOREIGN KEY REFERENCES Admin(UserID),
    ConcernID_FK INT FOREIGN KEY REFERENCES Concern(ConcernID)
);

-- 6. Create Feedback table
CREATE TABLE Feedback (
    FeedbackID INT IDENTITY(1,1) PRIMARY KEY,
    Rating INT NOT NULL,
    Comments VARCHAR(MAX),
    submission_time DATETIME2,
    ConcernID_FK INT FOREIGN KEY REFERENCES Concern(ConcernID),
    ReplyID_FK INT -- Explicitly mapped with ConstraintMode.NO_CONSTRAINT in code
);

-- 7. Create Notification table
CREATE TABLE Notification (
    NotificationID INT IDENTITY(1,1) PRIMARY KEY,
    Title VARCHAR(255),
    Message VARCHAR(MAX),
    Type VARCHAR(50),
    TargetAudience VARCHAR(100),
    IsRead BIT, -- Mapped to Boolean
    SentTime DATETIME2,
    AdminID_FK INT,
    StudentID_FK INT FOREIGN KEY REFERENCES Student(UserID),
    ConcernID_FK INT FOREIGN KEY REFERENCES Concern(ConcernID)
);

-- 8. Create Analytics_Report table
CREATE TABLE Analytics_Report (
    ReportID INT IDENTITY(1,1) PRIMARY KEY,
    TimePeriod VARCHAR(50),
    TotalConcerns INT,
    AvgResolutionTime DECIMAL(10,2),
    MostFrequentCategory VARCHAR(50),
    SentimentTrendScore DECIMAL(5,2),
    AdminID_FK INT,
    CreatedTime DATETIME2
);

-- 9. Create Student_Community_Post table
CREATE TABLE Student_Community_Post (
    PostID INT IDENTITY(1,1) PRIMARY KEY,
    Title VARCHAR(160) NOT NULL,
    Message VARCHAR(MAX) NOT NULL,
    Category VARCHAR(80) NOT NULL,
    Status VARCHAR(20) NOT NULL,
    CreatedTime DATETIME2 NOT NULL,
    UpdatedTime DATETIME2 NOT NULL,
    StudentID_FK INT NOT NULL FOREIGN KEY REFERENCES Student(UserID)
);

-- 10. Create Student_Community_Reply table
CREATE TABLE Student_Community_Reply (
    ReplyID INT IDENTITY(1,1) PRIMARY KEY,
    Message VARCHAR(MAX) NOT NULL,
    Status VARCHAR(20) NOT NULL,
    CreatedTime DATETIME2 NOT NULL,
    UpdatedTime DATETIME2 NOT NULL,
    PostID_FK INT NOT NULL FOREIGN KEY REFERENCES Student_Community_Post(PostID),
    StudentID_FK INT FOREIGN KEY REFERENCES Student(UserID),
    AdminName VARCHAR(100)
);

-- 11. Create Student_Community_Rules_Acceptance table
CREATE TABLE Student_Community_Rules_Acceptance (
    AcceptanceID INT IDENTITY(1,1) PRIMARY KEY,
    RulesVersion VARCHAR(20) NOT NULL,
    AcceptedAt DATETIME2 NOT NULL,
    StudentID_FK INT NOT NULL FOREIGN KEY REFERENCES Student(UserID)
);

-- 12. Create Student_Community_Moderation_Log table
CREATE TABLE Student_Community_Moderation_Log (
    LogID INT IDENTITY(1,1) PRIMARY KEY,
    ContentType VARCHAR(20) NOT NULL,
    Decision VARCHAR(10) NOT NULL,
    Reasons VARCHAR(1000),
    RiskScore INT,
    CreatedTime DATETIME2 NOT NULL,
    StudentID_FK INT FOREIGN KEY REFERENCES Student(UserID)
);
