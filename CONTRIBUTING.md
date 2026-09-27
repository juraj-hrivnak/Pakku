
# Contributing to Pakku

## Quickstart

To build the `pakku.jar` run the `:pakku-cli:jar` Gradle task (`gradlew :pakku-cli:jar`).

To run tests, run the `:pakku-core:test` Gradle task (`gradlew :pakku-core:test`).

## Important Info

When making a pull request, make sure also to:

- Write your changes to the [CHANGELOG's "Unreleased" section](CHANGELOG.md#unreleased), in a **human-readable** form.
- Update the documentation to reflect your changes.

## Creating Commands & Parameters (Clikt)

### Introduction

Pakku uses Clikt to handle the CLI. It is advised to [check out its docs](https://ajalt.github.io/clikt/).

##### Command Classes

Command classes should be named the same as the command itself.  
If the command is a subcommand, the class should be named `<parent-command><subcommand>`.  

For example: `CfgPrj`

##### Command Parameters

All variable names for command parameters (options, arguments, and flags) should end with:
- `Opt` for options
- `Arg` for arguments
- `Flag` for flags (`Boolean` options).

They should also be read-only (`val`) and `private`.  

If the parameter accepts multiple inputs, use the plural. (e.g.: `Opts`, `Args`)

```kt
// Parameters should always be read-only and private.
// Defining parameters using a chain of function calls is preferred.

private val exampleOpt by option("--example") // Consult if a short version `-e` of the option is a good idea
    .help("...") // Consider discussing the help message
    .int()
    .optionalValue(2)
    .default(0)

private val exampleArg by argument("example")
    .help("...")

private val exampleFlag by option("-e").flag()
```

### Documenting Commands

Documentation for commands can be generated automatically by running: `pakku --generate-docs`.

## Writing Tests

When creating a test please extend the `PakkuTest` class. 
It will ensure every test is run separately in the `build/test/` directory
and automatically handle the working path of Pakku and teardown of any created files while running the tests.

For assertions, we use Kotlin's `assert` with the [Power-assert](https://kotlinlang.org/docs/power-assert.html) plugin for better output; use `assertTrue`/`assertFalse` for booleans.

Example of a test:

```kt
class ExampleTest : PakkuTest()
{
    private val testFileName = "test_file.txt" // Define test data at the top of the class

    override suspend fun `set-up`() // Override this function to implement set up
    { 
        /** 
         * Create test files or directories. (Will tear down automatically.)
         * `fun createTestFile(vararg path: String)`
         * `fun createTestDir(vararg path: String)`
         */
        createTestFile(testFileName)
    }

    // Use backticks for test function names.
    @Test
    fun `test if file exists`() = runTest {
        val file = testFile(testFileName) // Reference test files using `fun testFile(vararg path: String): Path`

        assertTrue(file.exists())
    }
}
```
