import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }

        // 生成题目模式：需要参数 -n <题目数量> 和 -r <数值范围>
        if (hasParam(args, "-n") && hasParam(args, "-r")) {
            int count = Integer.parseInt(getParamValue(args, "-n"));
            int range = Integer.parseInt(getParamValue(args, "-r"));
            ExpressionGenerator generator = new ExpressionGenerator(range);
            // 使用Set确保题目唯一
            Set<String> exerciseSet = new HashSet<>();
            List<String> exercises = new ArrayList<>();
            List<String> answers = new ArrayList<>();
            int index = 1;
            while (exercises.size() < count) {
                Expression exp = generator.generateRandomExpression(0);
                String normalized = exp.getNormalizedString();
                // 去重判断
                if (!exerciseSet.contains(normalized)) {
                    exerciseSet.add(normalized);
                    exercises.add(index + ". " + exp.toString() + " =");
                    answers.add(index + ". " + AnswerEvaluator.evaluateToString(exp));
                    index++;
                }
            }
            FileManager.writeToFile("Exercises.txt", exercises);
            FileManager.writeToFile("Answers.txt", answers);
            System.out.println("生成题目成功，生成的题目存放在 Exercises.txt，答案存放在 Answers.txt");
        } else if (hasParam(args, "-e") && hasParam(args, "-a")) { // 批改模式
            String exerFile = getParamValue(args, "-e");
            String ansFile = getParamValue(args, "-a");
            Grader grader = new Grader(exerFile, ansFile);
            grader.grade();
            System.out.println("批改完成，结果存放在 Grade.txt");
        } else {
            printHelp();
            System.exit(1);
        }
    }

    private static boolean hasParam(String[] args, String param) {
        for (String arg : args) {
            if (arg.equals(param))
                return true;
        }
        return false;
    }

    private static String getParamValue(String[] args, String param) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(param) && i + 1 < args.length)
                return args[i + 1];
        }
        return null;
    }

    private static void printHelp() {
        System.out.println("使用说明：");
        System.out.println("生成题目模式：");
        System.out.println("    java Main -n <题目数量> -r <数值范围>");
        System.out.println("例如：java Main -n 10 -r 10");
        System.out.println("批改模式：");
        System.out.println("    java Main -e <Exercises.txt> -a <Answers.txt>");
    }

    // ************************************
    // Expression 及其子类定义
    // ************************************
    // 抽象表达式类
    static abstract class Expression {
        public abstract Fraction evaluate();
        public abstract String toString();
        public abstract String getNormalizedString();
    }

    // 自然数表达式
    static class NumberExpression extends Expression {
        private int number;
        public NumberExpression(int number) {
            this.number = number;
        }
        @Override
        public Fraction evaluate() {
            return new Fraction(number, 1);
        }
        @Override
        public String toString() {
            return String.valueOf(number);
        }
        @Override
        public String getNormalizedString() {
            return toString();
        }
    }

    // 真分数表达式（包括带分数）
    static class FractionExpression extends Expression {
        private int integerPart;  // 整数部分
        private int numerator;    // 分子
        private int denominator;  // 分母

        public FractionExpression(int integerPart, int numerator, int denominator) {
            this.integerPart = integerPart;
            this.numerator = numerator;
            this.denominator = denominator;
        }
        @Override
        public Fraction evaluate() {
            // 将带分数转换为假分数进行运算
            int totalNumerator = integerPart * denominator + numerator;
            return new Fraction(totalNumerator, denominator);
        }
        @Override
        public String toString() {
            if (integerPart > 0) {
                // 如果分子为0则只输出整数部分
                if(numerator == 0) {
                    return String.valueOf(integerPart);
                }
                return integerPart + "’" + numerator + "/" + denominator;
            } else {
                return numerator + "/" + denominator;
            }
        }
        @Override
        public String getNormalizedString() {
            // 可扩展成化简形式
            return toString();
        }
    }

    // 二元运算表达式
    static class BinaryExpression extends Expression {
        private Expression left;
        private Expression right;
        private char operator; // '+', '-', '×', '÷'

        public BinaryExpression(Expression left, Expression right, char operator) {
            this.left = left;
            this.right = right;
            this.operator = operator;
        }
        @Override
        public Fraction evaluate() {
            Fraction leftVal = left.evaluate();
            Fraction rightVal = right.evaluate();
            switch (operator) {
                case '+': return Fraction.add(leftVal, rightVal);
                case '-': return Fraction.subtract(leftVal, rightVal);
                case '×': return Fraction.multiply(leftVal, rightVal);
                case '÷': return Fraction.divide(leftVal, rightVal);
                default:
                    throw new RuntimeException("不支持的运算符: " + operator);
            }
        }
        @Override
        public String toString() {
            return left.toString() + " " + operator + " " + right.toString();
        }
        @Override
        public String getNormalizedString() {
            // 对于加法和乘法采用交换律进行标准化（左右子表达式字典序排序）
            if (operator == '+' || operator == '×') {
                String leftNorm = left.getNormalizedString();
                String rightNorm = right.getNormalizedString();
                if (leftNorm.compareTo(rightNorm) > 0) {
                    return "(" + rightNorm + " " + operator + " " + leftNorm + ")";
                }
            }
            return "(" + left.getNormalizedString() + " " + operator + " " + right.getNormalizedString() + ")";
        }
    }

    // ************************************
    // Fraction类：实现真分数的加减乘除及约分
    // ************************************
    static class Fraction implements Comparable<Fraction> {
        int numerator;
        int denominator;  // 保证 denominator > 0

        public Fraction(int numerator, int denominator) {
            if (denominator == 0)
                throw new ArithmeticException("分母不能为0");
            // 保证分母为正
            if (denominator < 0) {
                numerator = -numerator;
                denominator = -denominator;
            }
            int gcd = gcd(Math.abs(numerator), denominator);
            this.numerator = numerator / gcd;
            this.denominator = denominator / gcd;
        }
        
        public static Fraction add(Fraction a, Fraction b) {
            int num = a.numerator * b.denominator + b.numerator * a.denominator;
            int den = a.denominator * b.denominator;
            return new Fraction(num, den);
        }
        
        public static Fraction subtract(Fraction a, Fraction b) {
            int num = a.numerator * b.denominator - b.numerator * a.denominator;
            int den = a.denominator * b.denominator;
            return new Fraction(num, den);
        }
        
        public static Fraction multiply(Fraction a, Fraction b) {
            int num = a.numerator * b.numerator;
            int den = a.denominator * b.denominator;
            return new Fraction(num, den);
        }
        
        public static Fraction divide(Fraction a, Fraction b) {
            if (b.numerator == 0)
                throw new ArithmeticException("除数不能为0");
            int num = a.numerator * b.denominator;
            int den = a.denominator * b.numerator;
            return new Fraction(num, den);
        }
        
        public boolean isZero() {
            return numerator == 0;
        }
        
        @Override
        public int compareTo(Fraction o) {
            return Integer.compare(this.numerator * o.denominator, o.numerator * this.denominator);
        }
        
        // 将假分数转换为带分数形式字符串
        @Override
        public String toString() {
            if (denominator == 1) {
                return String.valueOf(numerator);
            }
            int absNum = Math.abs(numerator);
            if (absNum < denominator) {
                return numerator + "/" + denominator;
            } else {
                int integerPart = numerator / denominator;
                int remain = absNum % denominator;
                if (remain == 0) {
                    return String.valueOf(integerPart);
                } else {
                    return integerPart + "’" + remain + "/" + denominator;
                }
            }
        }
        
        private int gcd(int a, int b) {
            return b == 0 ? a : gcd(b, a % b);
        }
    }

    // ************************************
    // AnswerEvaluator：调用表达式的evaluate()方法，并转换为字符串
    // ************************************
    static class AnswerEvaluator {
        public static Fraction evaluate(Expression exp) {
            return exp.evaluate();
        }
        public static String evaluateToString(Expression exp) {
            return evaluate(exp).toString();
        }
    }

    // ************************************
    // ExpressionGenerator：随机生成表达式
    // ************************************
    static class ExpressionGenerator {
        private int range;
        private Random rand;

        public ExpressionGenerator(int range) {
            this.range = range;
            this.rand = new Random();
        }

        /**
         * level: 当前已使用的运算符个数
         */
        public Expression generateRandomExpression(int level) {
            // 运算符最多3个
            if (level >= 3 || rand.nextBoolean()) {
                return generateNumber();
            } else {
                char operator = randomOperator();
                Expression left, right;
                if (operator == '-') {
                    left = generateRandomExpression(level + 1);
                    // 保证 left >= right
                    do {
                        right = generateRandomExpression(level + 1);
                    } while (AnswerEvaluator.evaluate(left).compareTo(AnswerEvaluator.evaluate(right)) < 0);
                } else if (operator == '÷') {
                    left = generateRandomExpression(level + 1);
                    do {
                        right = generateRandomExpression(level + 1);
                    } while (AnswerEvaluator.evaluate(right).isZero());
                    // 此处简化：不做额外处理保证结果为真分数
                } else {
                    left = generateRandomExpression(level + 1);
                    right = generateRandomExpression(level + 1);
                }
                return new BinaryExpression(left, right, operator);
            }
        }

        private Expression generateNumber() {
            if (rand.nextBoolean()) {
                int num = rand.nextInt(range + 1);
                return new NumberExpression(num);
            } else {
                // 生成真分数，分母范围 2 ~ range
                int denominator = rand.nextInt(Math.max(1, range - 1)) + 2;
                int numerator = rand.nextInt(denominator); // 真分数
                if (rand.nextBoolean()) {
                    int integerPart = rand.nextInt(range + 1);
                    return new FractionExpression(integerPart, numerator, denominator);
                } else {
                    return new FractionExpression(0, numerator, denominator);
                }
            }
        }

        private char randomOperator() {
            char[] ops = {'+', '-', '×', '÷'};
            return ops[rand.nextInt(ops.length)];
        }
    }

    // ************************************
    // FileManager：读写文件
    // ************************************
    static class FileManager {
        public static void writeToFile(String filename, List<String> lines) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                for (String line : lines) {
                    writer.println(line);
                }
            } catch (IOException e) {
                System.err.println("写文件失败：" + e.getMessage());
            }
        }
        public static List<String> readFile(String filename) {
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    lines.add(line.trim());
                }
            } catch (IOException e) {
                System.err.println("读文件失败：" + e.getMessage());
            }
            return lines;
        }
    }

    // ************************************
    // Grader：批改题目
    // ************************************
    static class Grader {
        private String exerFile;
        private String ansFile;

        public Grader(String exerFile, String ansFile) {
            this.exerFile = exerFile;
            this.ansFile = ansFile;
        }

        public void grade() {
            List<String> exercises = FileManager.readFile(exerFile);
            List<String> userAnswers = FileManager.readFile(ansFile);
            List<String> correctIndices = new ArrayList<>();
            List<String> wrongIndices = new ArrayList<>();
            int index = 1;
            for (String exer : exercises) {
                // 假设题目格式为 "序号. 表达式 ="
                String expressionStr = exer.substring(exer.indexOf(" ") + 1, exer.lastIndexOf("=")).trim();
                Expression exp = ExpressionParser.parse(expressionStr);
                String correctAns = AnswerEvaluator.evaluateToString(exp);
                String userAns = parseUserAnswer(userAnswers.get(index - 1));
                if (correctAns.equals(userAns)) {
                    correctIndices.add(String.valueOf(index));
                } else {
                    wrongIndices.add(String.valueOf(index));
                }
                index++;
            }
            List<String> gradeResult = new ArrayList<>();
            gradeResult.add("Correct: " + correctIndices.size() + " (" + String.join(", ", correctIndices) + ")");
            gradeResult.add("Wrong: " + wrongIndices.size() + " (" + String.join(", ", wrongIndices) + ")");
            FileManager.writeToFile("Grade.txt", gradeResult);
        }

        private String parseUserAnswer(String line) {
            // 假设格式为 "序号. 答案"，提取答案部分
            int dotIndex = line.indexOf(".");
            return line.substring(dotIndex + 1).trim();
        }
    }

    // ************************************
    // ExpressionParser：将字符串解析为表达式（支持括号、加减乘除、数字和真分数）
    // ************************************
    static class ExpressionParser {
        private static List<String> tokens;
        private static int pos;

        public static Expression parse(String s) {
            tokens = tokenize(s);
            pos = 0;
            return parseExpression();
        }

        // 使用递归下降解析表达式
        private static Expression parseExpression() {
            Expression term = parseTerm();
            while (pos < tokens.size()) {
                String token = tokens.get(pos);
                if (token.equals("+") || token.equals("-")) {
                    pos++;
                    Expression nextTerm = parseTerm();
                    term = new BinaryExpression(term, nextTerm, token.charAt(0));
                } else {
                    break;
                }
            }
            return term;
        }

        private static Expression parseTerm() {
            Expression factor = parseFactor();
            while (pos < tokens.size()) {
                String token = tokens.get(pos);
                if (token.equals("×") || token.equals("÷")) {
                    pos++;
                    Expression nextFactor = parseFactor();
                    factor = new BinaryExpression(factor, nextFactor, token.charAt(0));
                } else {
                    break;
                }
            }
            return factor;
        }

        private static Expression parseFactor() {
            String token = tokens.get(pos);
            if (token.equals("(")) {
                pos++; // consume '('
                Expression exp = parseExpression();
                pos++; // consume ')'
                return exp;
            } else {
                pos++;
                return parseNumber(token);
            }
        }

        // 解析数字、真分数或带分数，如 "3", "1/2", "2’3/4"
        private static Expression parseNumber(String token) {
            if (token.contains("’")) {
                // 带分数形式：整数部分 + ’ + 分子/分母
                String[] parts = token.split("’");
                int integerPart = Integer.parseInt(parts[0]);
                String fractionPart = parts[1];
                String[] fracParts = fractionPart.split("/");
                int numerator = Integer.parseInt(fracParts[0]);
                int denominator = Integer.parseInt(fracParts[1]);
                return new FractionExpression(integerPart, numerator, denominator);
            } else if (token.contains("/")) {
                // 真分数形式：直接形式
                String[] fracParts = token.split("/");
                int numerator = Integer.parseInt(fracParts[0]);
                int denominator = Integer.parseInt(fracParts[1]);
                return new FractionExpression(0, numerator, denominator);
            } else {
                // 自然数
                int number = Integer.parseInt(token);
                return new NumberExpression(number);
            }
        }

        // 简单的分词器，按照空格和括号分隔
        private static List<String> tokenize(String s) {
            List<String> result = new ArrayList<>();
            // 利用正则表达式将括号、运算符和数字提取出来
            Pattern pattern = Pattern.compile("\\d+’\\d+/\\d+|\\d+/\\d+|\\d+|[+\\-×÷()]");
            Matcher matcher = pattern.matcher(s);
            while (matcher.find()) {
                result.add(matcher.group());
            }
            return result;
        }
    }
}
