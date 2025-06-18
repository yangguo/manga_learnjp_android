# Contributing to Manga Reader App

Thank you for your interest in contributing to the Manga Reader App! We welcome contributions from the community.

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check the issue list as you might find that the issue has already been reported. When creating a bug report, please include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples to demonstrate the steps**
- **Describe the behavior you observed and what behavior you expected**
- **Include screenshots if applicable**
- **Specify your Android version and device model**

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

- **Use a clear and descriptive title**
- **Provide a step-by-step description of the suggested enhancement**
- **Provide specific examples to demonstrate the enhancement**
- **Describe the current behavior and explain the behavior you expected**
- **Explain why this enhancement would be useful**

### Pull Requests

1. Fork the repository
2. Create a new branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes
4. Test your changes thoroughly
5. Commit your changes with a clear commit message
6. Push to your fork
7. Submit a pull request

## Development Guidelines

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

### Jetpack Compose Guidelines

- Use `@Composable` functions for UI components
- Follow the single responsibility principle
- Use `remember` for state that should survive recomposition
- Prefer `LaunchedEffect` for side effects
- Use `derivedStateOf` for computed state

### Architecture Guidelines

- Follow MVVM architecture pattern
- Keep ViewModels free of Android framework dependencies
- Use Repository pattern for data access
- Separate UI logic from business logic

### Testing

- Write unit tests for ViewModels and business logic
- Write UI tests for critical user flows
- Ensure all tests pass before submitting a PR
- Aim for good test coverage

### Commit Messages

Use clear and meaningful commit messages:

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line

Example:
```
Add dark theme support

- Implement dark theme colors
- Add theme toggle in settings
- Update UI components to support theming

Fixes #123
```

### Dependencies

- Avoid adding unnecessary dependencies
- Use well-maintained and popular libraries
- Update dependencies regularly
- Document why a dependency is needed

## Project Structure

When adding new features, follow the existing project structure:

```
app/src/main/java/com/example/manga_apk/
├── data/          # Data models, repositories, data sources
├── ui/            # UI components and screens
├── viewmodel/     # ViewModels
└── utils/         # Utility classes and extensions
```

## Code Review Process

1. All submissions require review before merging
2. Reviewers will check for:
   - Code quality and style
   - Architecture compliance
   - Test coverage
   - Performance implications
   - Security considerations

3. Address review feedback promptly
4. Be open to suggestions and constructive criticism

## Getting Help

If you need help with development:

- Check the existing documentation
- Look at similar implementations in the codebase
- Ask questions in GitHub issues
- Reach out to maintainers

## Recognition

Contributors will be recognized in the project's README and release notes.

Thank you for contributing to making this app better!