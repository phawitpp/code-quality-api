package main

import (
	"fmt"
	"github.com/gofiber/fiber/v2"
	"os"
	"os/exec"
	"strings"
)

type CodeRequest struct {
	Code string `json:"code"`
}

type LintResult struct {
	Line        string `json:"line"`
	Description string `json:"description"`
}

func main() {
	app := fiber.New()

	app.Post("/api/analyze", func(c *fiber.Ctx) error {
		var codeRequest CodeRequest
		if err := c.BodyParser(&codeRequest); err != nil {
			return c.Status(400).SendString("Invalid request body")
		}

		tmpFile, err := os.CreateTemp("", "*.c")
		if err != nil {
			return c.Status(500).SendString("Could not create temp file")
		}
		defer os.Remove(tmpFile.Name())
		if _, err := tmpFile.WriteString(codeRequest.Code); err != nil {
			return c.Status(500).SendString("Could not write code to file")
		}
		tmpFile.Close()

		cmd := exec.Command("clang-tidy", tmpFile.Name(), "--", "-std=c11")
		output, err := cmd.CombinedOutput()
		if err != nil && len(output) == 0 {
			return c.Status(500).SendString("Clang-tidy failed: " + err.Error())
		}

		lines := strings.Split(string(output), "\n")
		var lintResults []LintResult

		for _, line := range lines {
			if strings.Contains(line, "warning") || strings.Contains(line, "error") {

				parts := strings.SplitN(line, ":", 4)
				if len(parts) >= 4 {
					lintResults = append(lintResults, LintResult{
						Line:        fmt.Sprintf("%s:%s", parts[1], parts[2]),
						Description: strings.TrimSpace(parts[3]),
					})
				}
			}
		}

		response := fiber.Map{
			"results": lintResults,
			"message": "Code analyzed successfully",
		}
		return c.JSON(response)
	})

	fmt.Println("Server is running on port 8082...")
	app.Listen(":8082")
}
