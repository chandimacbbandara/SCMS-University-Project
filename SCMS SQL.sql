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

select *
from Admin_reply

select *
from Notification