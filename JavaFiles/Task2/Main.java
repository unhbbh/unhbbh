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

        // ������Ŀģʽ����Ҫ���� -n <��Ŀ����> �� -r <��ֵ��Χ>
        if (hasParam(args, "-n") && hasParam(args, "-r")) {
            int count = Integer.parseInt(getParamValue(args, "-n"));
            int range = Integer.parseInt(getParamValue(args, "-r"));
            ExpressionGenerator generator = new ExpressionGenerator(range);
            // ʹ��Setȷ����ĿΨһ
            Set<String> exerciseSet = new HashSet<>();
            List<String> exercises = new ArrayList<>();
            List<String> answers = new ArrayList<>();
            int index = 1;
            while (exercises.size() < count) {
                Expression exp = generator.generateRandomExpression(0);
                String normalized = exp.getNormalizedString();
                // ȥ���ж�
                if (!exerciseSet.contains(normalized)) {
                    exerciseSet.add(normalized);
                    exercises.add(index + ". " + exp.toString() + " =");
                    answers.add(index + ". " + AnswerEvaluator.evaluateToString(exp));
                    index++;
                }
            }
            FileManager.writeToFile("Exercises.txt", exercises);
            FileManager.writeToFile("Answers.txt", answers);
            System.out.println("������Ŀ�ɹ������ɵ���Ŀ����� Exercises.txt���𰸴���� Answers.txt");
        } else if (hasParam(args, "-e") && hasParam(args, "-a")) { // ����ģʽ
            String exerFile = getParamValue(args, "-e");
            String ansFile = getParamValue(args, "-a");
            Grader grader = new Grader(exerFile, ansFile);
            grader.grade();
            System.out.println("������ɣ��������� Grade.txt");
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
        System.out.println("ʹ��˵����");
        System.out.println("������Ŀģʽ��");
        System.out.println("    java Main -n <��Ŀ����> -r <��ֵ��Χ>");
        System.out.println("���磺java Main -n 10 -r 10");
        System.out.println("����ģʽ��");
        System.out.println("    java Main -e <Exercises.txt> -a <Answers.txt>");
    }

    // ************************************
    // Expression �������ඨ��
    // ************************************
    // ������ʽ��
    static abstract class Expression {
        public abstract Fraction evaluate();
        public abstract String toString();
        public abstract String getNormalizedString();
    }

    // ��Ȼ�����ʽ
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

    // ��������ʽ��������������
    static class FractionExpression extends Expression {
        private int integerPart;  // ��������
        private int numerator;    // ����
        private int denominator;  // ��ĸ

        public FractionExpression(int integerPart, int numerator, int denominator) {
            this.integerPart = integerPart;
            this.numerator = numerator;
            this.denominator = denominator;
        }
        @Override
        public Fraction evaluate() {
            // ��������ת��Ϊ�ٷ�����������
            int totalNumerator = integerPart * denominator + numerator;
            return new Fraction(totalNumerator, denominator);
        }
        @Override
        public String toString() {
            if (integerPart > 0) {
                // �������Ϊ0��ֻ�����������
                if(numerator == 0) {
                    return String.valueOf(integerPart);
                }
                return integerPart + "��" + numerator + "/" + denominator;
            } else {
                return numerator + "/" + denominator;
            }
        }
        @Override
        public String getNormalizedString() {
            // ����չ�ɻ�����ʽ
            return toString();
        }
    }

    // ��Ԫ������ʽ
    static class BinaryExpression extends Expression {
        private Expression left;
        private Expression right;
        private char operator; // '+', '-', '��', '��'

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
                case '��': return Fraction.multiply(leftVal, rightVal);
                case '��': return Fraction.divide(leftVal, rightVal);
                default:
                    throw new RuntimeException("��֧�ֵ������: " + operator);
            }
        }
        @Override
        public String toString() {
            return left.toString() + " " + operator + " " + right.toString();
        }
        @Override
        public String getNormalizedString() {
            // ���ڼӷ��ͳ˷����ý����ɽ��б�׼���������ӱ��ʽ�ֵ�������
            if (operator == '+' || operator == '��') {
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
    // Fraction�ࣺʵ��������ļӼ��˳���Լ��
    // ************************************
    static class Fraction implements Comparable<Fraction> {
        int numerator;
        int denominator;  // ��֤ denominator > 0

        public Fraction(int numerator, int denominator) {
            if (denominator == 0)
                throw new ArithmeticException("��ĸ����Ϊ0");
            // ��֤��ĸΪ��
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
                throw new ArithmeticException("��������Ϊ0");
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
        
        // ���ٷ���ת��Ϊ��������ʽ�ַ���
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
                    return integerPart + "��" + remain + "/" + denominator;
                }
            }
        }
        
        private int gcd(int a, int b) {
            return b == 0 ? a : gcd(b, a % b);
        }
    }

    // ************************************
    // AnswerEvaluator�����ñ��ʽ��evaluate()��������ת��Ϊ�ַ���
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
    // ExpressionGenerator��������ɱ��ʽ
    // ************************************
    static class ExpressionGenerator {
        private int range;
        private Random rand;

        public ExpressionGenerator(int range) {
            this.range = range;
            this.rand = new Random();
        }

        /**
         * level: ��ǰ��ʹ�õ����������
         */
        public Expression generateRandomExpression(int level) {
            // ��������3��
            if (level >= 3 || rand.nextBoolean()) {
                return generateNumber();
            } else {
                char operator = randomOperator();
                Expression left, right;
                if (operator == '-') {
                    left = generateRandomExpression(level + 1);
                    // ��֤ left >= right
                    do {
                        right = generateRandomExpression(level + 1);
                    } while (AnswerEvaluator.evaluate(left).compareTo(AnswerEvaluator.evaluate(right)) < 0);
                } else if (operator == '��') {
                    left = generateRandomExpression(level + 1);
                    do {
                        right = generateRandomExpression(level + 1);
                    } while (AnswerEvaluator.evaluate(right).isZero());
                    // �˴��򻯣��������⴦��֤���Ϊ�����
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
                // �������������ĸ��Χ 2 ~ range
                int denominator = rand.nextInt(Math.max(1, range - 1)) + 2;
                int numerator = rand.nextInt(denominator); // �����
                if (rand.nextBoolean()) {
                    int integerPart = rand.nextInt(range + 1);
                    return new FractionExpression(integerPart, numerator, denominator);
                } else {
                    return new FractionExpression(0, numerator, denominator);
                }
            }
        }

        private char randomOperator() {
            char[] ops = {'+', '-', '��', '��'};
            return ops[rand.nextInt(ops.length)];
        }
    }

    // ************************************
    // FileManager����д�ļ�
    // ************************************
    static class FileManager {
        public static void writeToFile(String filename, List<String> lines) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                for (String line : lines) {
                    writer.println(line);
                }
            } catch (IOException e) {
                System.err.println("д�ļ�ʧ�ܣ�" + e.getMessage());
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
                System.err.println("���ļ�ʧ�ܣ�" + e.getMessage());
            }
            return lines;
        }
    }

    // ************************************
    // Grader��������Ŀ
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
                // ������Ŀ��ʽΪ "���. ���ʽ ="
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
            // �����ʽΪ "���. ��"����ȡ�𰸲���
            int dotIndex = line.indexOf(".");
            return line.substring(dotIndex + 1).trim();
        }
    }

    // ************************************
    // ExpressionParser�����ַ�������Ϊ���ʽ��֧�����š��Ӽ��˳������ֺ��������
    // ************************************
    static class ExpressionParser {
        private static List<String> tokens;
        private static int pos;

        public static Expression parse(String s) {
            tokens = tokenize(s);
            pos = 0;
            return parseExpression();
        }

        // ʹ�õݹ��½��������ʽ
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
                if (token.equals("��") || token.equals("��")) {
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

        // �������֡����������������� "3", "1/2", "2��3/4"
        private static Expression parseNumber(String token) {
            if (token.contains("��")) {
                // ��������ʽ���������� + �� + ����/��ĸ
                String[] parts = token.split("��");
                int integerPart = Integer.parseInt(parts[0]);
                String fractionPart = parts[1];
                String[] fracParts = fractionPart.split("/");
                int numerator = Integer.parseInt(fracParts[0]);
                int denominator = Integer.parseInt(fracParts[1]);
                return new FractionExpression(integerPart, numerator, denominator);
            } else if (token.contains("/")) {
                // �������ʽ��ֱ����ʽ
                String[] fracParts = token.split("/");
                int numerator = Integer.parseInt(fracParts[0]);
                int denominator = Integer.parseInt(fracParts[1]);
                return new FractionExpression(0, numerator, denominator);
            } else {
                // ��Ȼ��
                int number = Integer.parseInt(token);
                return new NumberExpression(number);
            }
        }

        // �򵥵ķִ��������տո�����ŷָ�
        private static List<String> tokenize(String s) {
            List<String> result = new ArrayList<>();
            // ����������ʽ�����š��������������ȡ����
            Pattern pattern = Pattern.compile("\\d+��\\d+/\\d+|\\d+/\\d+|\\d+|[+\\-����()]");
            Matcher matcher = pattern.matcher(s);
            while (matcher.find()) {
                result.add(matcher.group());
            }
            return result;
        }
    }
}
