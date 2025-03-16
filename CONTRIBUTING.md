# Contributing to SQL Utils

First off, thank you for considering contributing to SQL Utils! It's people like you that make this project better for everyone.

Following these guidelines helps to communicate that you respect the time of the developers managing and developing this open source project. In return, they should reciprocate that respect in addressing your issue, assessing changes, and helping you finalize your pull requests.

## Code of Conduct

This project and everyone participating in it is governed by the [SQL Utils Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [project maintainer email].

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

**Before submitting a bug report:**

* Check the documentation for tips on using the library correctly.
* Check if the bug has already been reported by searching on GitHub under [Issues](https://github.com/akhil7000/sql-utils/issues).

**How to submit a good bug report:**

Bugs are tracked as [GitHub issues](https://github.com/akhil7000/sql-utils/issues). Create an issue and provide the following information:

* Use a clear and descriptive title.
* Describe the exact steps which reproduce the problem.
* Provide specific examples to demonstrate the steps.
* Describe the behavior you observed after following the steps.
* Explain which behavior you expected to see instead and why.
* Include details about your environment (OS, Java version, etc.).
* Include any relevant logs or stack traces.

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion, including completely new features and minor improvements to existing functionality.

**Before submitting an enhancement suggestion:**

* Check if the enhancement has already been suggested by searching on GitHub under [Issues](https://github.com/akhil7000/sql-utils/issues).

**How to submit a good enhancement suggestion:**

Enhancement suggestions are tracked as [GitHub issues](https://github.com/akhil7000/sql-utils/issues). Create an issue and provide the following information:

* Use a clear and descriptive title.
* Provide a step-by-step description of the suggested enhancement.
* Provide specific examples to demonstrate the steps.
* Describe the current behavior and explain which behavior you expected to see instead and why.
* Explain why this enhancement would be useful to most users.

### Pull Requests

* Fill in the required template
* Follow the Java code style
* Include appropriate test cases
* Update the documentation, if necessary
* End all files with a newline
* Make sure your code lints (no compiler warnings or errors)

## Development Process

### Setting Up the Development Environment

1. Fork the repository
2. Clone your fork locally
3. Set up the development dependencies
4. Create a new branch for your changes

```bash
git clone https://github.com/yourusername/sql-utils.git
cd sql-utils
git checkout -b feature/your-feature-name
```

### Building and Testing

```bash
# Build the project
mvn clean install

# Run tests
mvn test
```

## Style Guides

### Git Commit Messages

* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Limit the first line to 72 characters or less
* Reference issues and pull requests liberally after the first line

### Java Style Guide

* Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
* Use 4 spaces for indentation
* Prefer composition over inheritance

### Documentation Style Guide

* Use Markdown for documentation
* Reference methods and classes in backticks, like `QueryUtil.getQuery()`
* Include code examples for better understanding

## Additional Notes

### Issue and Pull Request Labels

This project uses labels to categorize issues and pull requests:

* `bug`: Something isn't working
* `enhancement`: New feature or request
* `documentation`: Improvements to documentation
* `good first issue`: Good for newcomers
* `help wanted`: Extra attention is needed 