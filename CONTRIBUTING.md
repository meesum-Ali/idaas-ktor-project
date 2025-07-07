# Contribution Guidelines

We're excited that you're interested in contributing to the IDaaS Ktor Project! This document provides guidelines and steps for contributing.

## Getting Started

### Development Setup

1. **Prerequisites:**
   - JDK 11 or higher
   - Docker and Docker Compose
   - Gradle (optional, wrapper included)

2. **Local Setup:**
   ```bash
   # Clone your fork of the repository
   git clone https://github.com/YOUR-USERNAME/idaas-ktor-project.git
   cd idaas-ktor-project

   # Set up upstream remote
   git remote add upstream https://github.com/ORIGINAL-OWNER/idaas-ktor-project.git
   ```

3. **Build and Test:**
   ```bash
   ./gradlew build
   ./gradlew test
   ```

## Development Workflow

1. **Create a Feature Branch:**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

2. **Keep Your Branch Updated:**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

3. **Commit Guidelines:**
   - Write clear, concise commit messages
   - Use conventional commits format:
     - `feat: add new feature`
     - `fix: resolve bug issue`
     - `docs: update documentation`
     - `test: add test cases`
     - `refactor: improve code structure`

## Coding Standards

1. **Kotlin Style Guide:**
   - Follow Kotlin coding conventions
   - Use meaningful variable and function names
   - Document public APIs
   - Keep functions focused and concise

2. **Testing Requirements:**
   - Write unit tests for new features
   - Ensure all tests pass before submitting
   - Include integration tests when necessary
   - Maintain test coverage above 80%

3. **Code Quality:**
   - Use proper error handling
   - Follow SOLID principles
   - Write self-documenting code
   - Include comments for complex logic

## Pull Request Process

1. **Before Submitting:**
   - Update documentation if needed
   - Run all tests locally
   - Format your code
   - Resolve merge conflicts

2. **PR Guidelines:**
   - Fill in the PR template completely
   - Link related issues
   - Provide clear description of changes
   - Include screenshots for UI changes

3. **Review Process:**
   - Address reviewer comments
   - Keep the PR focused on a single change
   - Be responsive to feedback
   - Update your PR when needed

## Additional Resources

- Project Documentation: Check the `docs/` directory
- Issue Guidelines: Review existing issues for examples
- Discord Channel: Join our community chat

## Need Help?

Feel free to:
- Open an issue for questions
- Ask in our Discord channel
- Tag maintainers in comments

Thank you for contributing to make this project better!