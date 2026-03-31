-- 1. User Table (Supertype - Wrapped in brackets because 'User' is reserved)
CREATE TABLE [User] (
    UserID INT PRIMARY KEY,
    Email VARCHAR(255) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL,
    Gender VARCHAR(10),
    Phone_number VARCHAR(15),
    First_Name VARCHAR(100),
    Last_Name VARCHAR(100),
    Address_1st_Lane VARCHAR(255),
    Address_2nd_Lane VARCHAR(255),
    Address_3rd_Lane VARCHAR(255)
);

-- 2. Student Table (Subtype)
CREATE TABLE Student (
    UserID INT PRIMARY KEY,
    StudentID VARCHAR(20) UNIQUE NOT NULL,
    DOB DATE,
    StudentDPhoto VARBINARY(MAX),
    Category VARCHAR(50)
);

-- 3. Admin Table (Subtype)
CREATE TABLE Admin (
    UserID INT PRIMARY KEY,
    StaffID VARCHAR(20) UNIQUE NOT NULL
);

-- 4. Concern Table
CREATE TABLE Concern (
    ConcernID INT PRIMARY KEY,
    Subject VARCHAR(255) NOT NULL,
    Message VARCHAR(MAX) NOT NULL,
    Evidence VARBINARY(MAX),
    AI_Priority_Level VARCHAR(20),
    Status VARCHAR(50),
    CreatedTime DATETIME DEFAULT GETDATE(),
    StudentID_FK INT,
    AdminID_FK INT
);

-- 5. Admin_reply Table
CREATE TABLE Admin_reply (
    ReplyID INT PRIMARY KEY,
    Reply_Message VARCHAR(MAX) NOT NULL,
    Resolution_Screenshot VARBINARY(MAX),
    Reply_Time DATETIME DEFAULT GETDATE(),
    AdminID_FK INT,
    ConcernID_FK INT
);

-- 6. Feedback Table
CREATE TABLE Feedback (
    FeedbackID INT PRIMARY KEY,
    Rating INT CHECK (Rating BETWEEN 1 AND 5),
    Comments VARCHAR(MAX),
    SubmissionTime DATETIME DEFAULT GETDATE(),
    ConcernID_FK INT
);

-- 7. Notification Table
CREATE TABLE Notification (
    NotificationID INT PRIMARY KEY,
    Title VARCHAR(255),
    Message VARCHAR(MAX),
    Type VARCHAR(50),
    TargetAudience VARCHAR(100),
    SentTime DATETIME DEFAULT GETDATE(),
    AdminID_FK INT
);

-- 8. Analytics_Report Table
CREATE TABLE Analytics_Report (
    ReportID INT PRIMARY KEY,
    TimePeriod VARCHAR(50),
    TotalConcerns INT,
    AvgResolutionTime DECIMAL(10,2),
    MostFrequentCategory VARCHAR(50),
    SentimentTrendScore DECIMAL(5,2),
    AdminID_FK INT
);


-- Linking Subtypes to Supertype (ISA Relationship)
ALTER TABLE Student ADD CONSTRAINT FK_Student_User FOREIGN KEY (UserID) REFERENCES [User](UserID);
ALTER TABLE Admin ADD CONSTRAINT FK_Admin_User FOREIGN KEY (UserID) REFERENCES [User](UserID);

-- Concern Relationships
ALTER TABLE Concern ADD CONSTRAINT FK_Concern_Student FOREIGN KEY (StudentID_FK) REFERENCES Student(UserID);
ALTER TABLE Concern ADD CONSTRAINT FK_Concern_Admin FOREIGN KEY (AdminID_FK) REFERENCES Admin(UserID);

-- Admin Reply Relationships
ALTER TABLE Admin_reply ADD CONSTRAINT FK_Reply_Admin FOREIGN KEY (AdminID_FK) REFERENCES Admin(UserID);
ALTER TABLE Admin_reply ADD CONSTRAINT FK_Reply_Concern FOREIGN KEY (ConcernID_FK) REFERENCES Concern(ConcernID);

-- Feedback Relationship
ALTER TABLE Feedback ADD CONSTRAINT FK_Feedback_Concern FOREIGN KEY (ConcernID_FK) REFERENCES Concern(ConcernID);

-- Notification Relationship
ALTER TABLE Notification ADD CONSTRAINT FK_Notification_Admin FOREIGN KEY (AdminID_FK) REFERENCES Admin(UserID);

-- Analytics Report Relationship
ALTER TABLE Analytics_Report ADD CONSTRAINT FK_Report_Admin FOREIGN KEY (AdminID_FK) REFERENCES Admin(UserID);


select *
from concern

select *
from [User]

select *
from student

SELECT *
FROM Admin

select *
from Admin_reply

SELECT *
FROM Feedback

select *
from Notification

SELECT *
FROM Analytics_Report

select *  
FROM faqs

select *
from tips


/* =============================
   Student Community Talk Module
   ============================= */

CREATE TABLE Student_Community_Post (
    PostID INT IDENTITY(1,1) PRIMARY KEY,
    Title VARCHAR(160) NOT NULL,
    Message VARCHAR(MAX) NOT NULL,
    Category VARCHAR(80) NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CreatedTime DATETIME NOT NULL DEFAULT GETDATE(),
    UpdatedTime DATETIME NOT NULL DEFAULT GETDATE(),
    StudentID_FK INT NOT NULL,
    CONSTRAINT FK_CommunityPost_Student FOREIGN KEY (StudentID_FK) REFERENCES Student(UserID)
);

CREATE TABLE Student_Community_Reply (
    ReplyID INT IDENTITY(1,1) PRIMARY KEY,
    Message VARCHAR(MAX) NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CreatedTime DATETIME NOT NULL DEFAULT GETDATE(),
    UpdatedTime DATETIME NOT NULL DEFAULT GETDATE(),
    PostID_FK INT NOT NULL,
    StudentID_FK INT NOT NULL,
    CONSTRAINT FK_CommunityReply_Post FOREIGN KEY (PostID_FK) REFERENCES Student_Community_Post(PostID),
    CONSTRAINT FK_CommunityReply_Student FOREIGN KEY (StudentID_FK) REFERENCES Student(UserID)
);

CREATE TABLE Student_Community_Rules_Acceptance (
    AcceptanceID INT IDENTITY(1,1) PRIMARY KEY,
    RulesVersion VARCHAR(20) NOT NULL,
    AcceptedAt DATETIME NOT NULL DEFAULT GETDATE(),
    StudentID_FK INT NOT NULL UNIQUE,
    CONSTRAINT FK_CommunityRules_Student FOREIGN KEY (StudentID_FK) REFERENCES Student(UserID)
);

CREATE TABLE Student_Community_Moderation_Log (
    LogID INT IDENTITY(1,1) PRIMARY KEY,
    ContentType VARCHAR(20) NOT NULL,
    Decision VARCHAR(10) NOT NULL,
    Reasons VARCHAR(1000),
    RiskScore INT,
    CreatedTime DATETIME NOT NULL DEFAULT GETDATE(),
    StudentID_FK INT NULL,
    CONSTRAINT FK_CommunityModLog_Student FOREIGN KEY (StudentID_FK) REFERENCES Student(UserID)
);

CREATE INDEX IDX_CommunityPost_Status_CreatedTime
ON Student_Community_Post(Status, CreatedTime DESC);

CREATE INDEX IDX_CommunityReply_Post_Status
ON Student_Community_Reply(PostID_FK, Status, CreatedTime ASC);


-- Helpful verification queries
SELECT * FROM Student_Community_Post ORDER BY CreatedTime DESC;
SELECT * FROM Student_Community_Reply ORDER BY CreatedTime DESC;
SELECT * FROM Student_Community_Rules_Acceptance ORDER BY AcceptedAt DESC;
SELECT * FROM Student_Community_Moderation_Log ORDER BY CreatedTime DESC;ALTER TABLE Student_Community_Reply ALTER COLUMN StudentID_FK INT NULL;


/* ========================================
   Concern Physical Meeting Scheduling Flow
   ======================================== */

ALTER TABLE Concern ADD Meeting_Status VARCHAR(60);
ALTER TABLE Concern ADD Meeting_Booked_Start_Time DATETIME;
ALTER TABLE Concern ADD Meeting_Booked_End_Time DATETIME;
ALTER TABLE Concern ADD Meeting_Booked_At DATETIME;

CREATE TABLE Concern_Meeting_Proposal (
    ProposalID INT IDENTITY(1,1) PRIMARY KEY,
    ConcernID_FK INT NOT NULL,
    AdminID_FK INT NOT NULL,
    Proposal_Status VARCHAR(60) NOT NULL DEFAULT 'PENDING_STUDENT_SELECTION',
    Admin_Note VARCHAR(MAX),
    Student_Response_Note VARCHAR(MAX),
    Created_Time DATETIME NOT NULL DEFAULT GETDATE(),
    Responded_Time DATETIME NULL,
    CONSTRAINT FK_MeetingProposal_Concern FOREIGN KEY (ConcernID_FK) REFERENCES Concern(ConcernID),
    CONSTRAINT FK_MeetingProposal_Admin FOREIGN KEY (AdminID_FK) REFERENCES Admin(UserID)
);

CREATE TABLE Concern_Meeting_Slot (
    SlotID INT IDENTITY(1,1) PRIMARY KEY,
    ProposalID_FK INT NOT NULL,
    Start_Time DATETIME NOT NULL,
    End_Time DATETIME NOT NULL,
    Slot_Status VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT FK_MeetingSlot_Proposal FOREIGN KEY (ProposalID_FK) REFERENCES Concern_Meeting_Proposal(ProposalID)
);

CREATE INDEX IDX_MeetingProposal_Concern ON Concern_Meeting_Proposal(ConcernID_FK, Created_Time DESC);
CREATE INDEX IDX_MeetingSlot_Proposal ON Concern_Meeting_Slot(ProposalID_FK, Start_Time ASC);

SELECT * FROM Concern_Meeting_Proposal ORDER BY Created_Time DESC;
SELECT * FROM Concern_Meeting_Slot ORDER BY Start_Time ASC;

