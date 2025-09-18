Document Generator
A powerful tool for generating multiple documents from Word templates using Excel data sources. Perfect for creating personalized letters, certificates, invoices, or any document that needs to be mass-produced with different data.
Features

Template-based generation: Use Word (DOCX) files as templates
Excel integration: Pull data from XLSX files
Batch processing: Generate hundreds of documents at once
Single document mode: Create individual documents with manual input
Simple placeholder system: Use square brackets for data fields
Smart date formatting: Automatic date conversion to dd.MM.yyyy format

How It Works

Create a Word template with placeholders in square brackets
Prepare an Excel file with your data
Select template and data source
Generate all documents automatically

Getting Started
1. Creating a Template

Create your document in Word (DOCX format)
Replace variable content with column names in square brackets
Save the template

Example:
Original: "Congratulations, Ivanov Ivan Ivanovich!"
Template: "Congratulations, [FULL_NAME]!"
Important: Column names are case-sensitive. [FULL_NAME] will only match a column named exactly "FULL_NAME".
2. Preparing Your Data

Create an Excel file (XLSX format)
Put column names in the first row
Fill subsequent rows with your data

Example Excel structure:
FULL_NAMEDATEAMOUNTJohn Smith01.01.2024$100Jane Doe02.01.2024$150
Note: Dates are automatically formatted as dd.MM.yyyy (e.g., 01.01.2001).
3. Using the Application
Adding Templates

Click "Add template" button
Select your prepared DOCX template
Template will be stored and available for selection

Generating Multiple Documents

Select a template from the upper window
Click "Generate documents"
Choose your Excel data file
Select output folder
Done! All documents will be created automatically

Generating Single Documents

Select a template
Click "Generate a single document"
Fill in the prompted fields manually
Save the result

Requirements

Word templates in DOCX format
Excel data files in XLSX format
Matching column names between template and data (case-sensitive)

Tips

Keep column names simple and descriptive
Ensure exact case matching between template placeholders and Excel headers
Use the first row of Excel for column headers only
Test with a small dataset first

Support
If you encounter issues or have questions, please open an issue in this repository.
