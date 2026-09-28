package org.example.springbootdemo.agent;

/**
 * 步骤执行结果
 */
public class StepResult {

    private boolean success;
    private String message;

    private StepResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static StepResult success(String message) {
        return new StepResult(true, message);
    }

    public static StepResult fail(String message) {
        return new StepResult(false, message);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
