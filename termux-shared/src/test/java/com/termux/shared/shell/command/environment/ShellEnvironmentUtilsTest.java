package com.termux.shared.shell.command.environment;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShellEnvironmentUtilsTest {

    @Test
    public void isValidEnvironmentVariableNameAcceptsAlphaUnderscoreStart() {
        Assert.assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableName("HOME"));
        Assert.assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableName("_private"));
        Assert.assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableName("PATH_2"));
    }

    @Test
    public void isValidEnvironmentVariableNameRejectsNull() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName(null));
    }

    @Test
    public void isValidEnvironmentVariableNameRejectsLeadingDigit() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName("2PATH"));
    }

    @Test
    public void isValidEnvironmentVariableNameRejectsInvalidCharacters() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName("NAME-WITH-DASH"));
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName("HAS SPACE"));
    }

    @Test
    public void isValidEnvironmentVariableNameRejectsNullByte() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableName("NAME\0"));
    }

    @Test
    public void isValidEnvironmentVariableValueAcceptsNonNullWithoutNullByte() {
        Assert.assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableValue("any value"));
        Assert.assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableValue(""));
    }

    @Test
    public void isValidEnvironmentVariableValueRejectsNull() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableValue(null));
    }

    @Test
    public void isValidEnvironmentVariableValueRejectsNullByte() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableValue("a\0b"));
    }

    @Test
    public void isValidEnvironmentVariableNameValuePairTrueForValidPair() {
        Assert.assertTrue(ShellEnvironmentUtils.isValidEnvironmentVariableNameValuePair("HOME", "/home", false));
    }

    @Test
    public void isValidEnvironmentVariableNameValuePairFalseForInvalidName() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableNameValuePair("2X", "v", false));
    }

    @Test
    public void isValidEnvironmentVariableNameValuePairFalseForInvalidValue() {
        Assert.assertFalse(ShellEnvironmentUtils.isValidEnvironmentVariableNameValuePair("X", null, false));
    }

    @Test
    public void convertEnvironmentToEnvironProducesNameEqualsValueEntries() {
        HashMap<String, String> map = new HashMap<>();
        map.put("HOME", "/home");
        List<String> environ = ShellEnvironmentUtils.convertEnvironmentToEnviron(map);
        Assert.assertEquals(1, environ.size());
        Assert.assertEquals("HOME=/home", environ.get(0));
    }

    @Test
    public void convertEnvironmentToEnvironSkipsInvalidNames() {
        HashMap<String, String> map = new HashMap<>();
        map.put("2INVALID", "x");
        Assert.assertTrue(ShellEnvironmentUtils.convertEnvironmentToEnviron(map).isEmpty());
    }

    @Test
    public void convertEnvironmentMapToEnvironmentVariableListCreatesUnescapedVariables() {
        HashMap<String, String> map = new HashMap<>();
        map.put("KEY", "value");
        List<ShellEnvironmentVariable> list =
            ShellEnvironmentUtils.convertEnvironmentMapToEnvironmentVariableList(map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("KEY", list.get(0).name);
        Assert.assertEquals("value", list.get(0).value);
        Assert.assertFalse(list.get(0).escaped);
    }

    @Test
    public void convertEnvironmentToDotEnvFileWritesSortedExportLines() {
        List<ShellEnvironmentVariable> list = new ArrayList<>();
        list.add(new ShellEnvironmentVariable("ZED", "z", false));
        list.add(new ShellEnvironmentVariable("ALPHA", "a", false));
        String dotEnv = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(list);
        Assert.assertEquals("export ALPHA=\"a\"\nexport ZED=\"z\"\n", dotEnv);
    }

    @Test
    public void convertEnvironmentToDotEnvFileEscapesSpecialCharactersWhenNotEscaped() {
        List<ShellEnvironmentVariable> list = new ArrayList<>();
        list.add(new ShellEnvironmentVariable("KEY", "a\"b$c`d\\e", false));
        String dotEnv = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(list);
        Assert.assertEquals("export KEY=\"a\\\"b\\$c\\`d\\\\e\"\n", dotEnv);
    }

    @Test
    public void convertEnvironmentToDotEnvFileKeepsValueLiteralWhenEscaped() {
        List<ShellEnvironmentVariable> list = new ArrayList<>();
        list.add(new ShellEnvironmentVariable("KEY", "already\\\"escaped", true));
        String dotEnv = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(list);
        Assert.assertEquals("export KEY=\"already\\\"escaped\"\n", dotEnv);
    }

    @Test
    public void putToEnvIfSetStoresStringValueWhenPresent() {
        HashMap<String, String> environment = new HashMap<>();
        ShellEnvironmentUtils.putToEnvIfSet(environment, "KEY", "value");
        Assert.assertEquals("value", environment.get("KEY"));
    }

    @Test
    public void putToEnvIfSetIgnoresNullStringValue() {
        HashMap<String, String> environment = new HashMap<>();
        ShellEnvironmentUtils.putToEnvIfSet(environment, "KEY", (String) null);
        Assert.assertFalse(environment.containsKey("KEY"));
    }

    @Test
    public void putToEnvIfSetStoresBooleanValueAsString() {
        HashMap<String, String> environment = new HashMap<>();
        ShellEnvironmentUtils.putToEnvIfSet(environment, "FLAG", Boolean.TRUE);
        Assert.assertEquals("true", environment.get("FLAG"));
    }

    @Test
    public void putToEnvIfSetIgnoresNullBooleanValue() {
        HashMap<String, String> environment = new HashMap<>();
        ShellEnvironmentUtils.putToEnvIfSet(environment, "FLAG", (Boolean) null);
        Assert.assertFalse(environment.containsKey("FLAG"));
    }

    @Test
    public void putToEnvIfInSystemEnvCopiesExistingSystemVariable() {
        String existingName = System.getenv().keySet().iterator().next();
        HashMap<String, String> environment = new HashMap<>();
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, existingName);
        Assert.assertEquals(System.getenv(existingName), environment.get(existingName));
    }

    @Test
    public void putToEnvIfInSystemEnvIgnoresUnknownVariable() {
        HashMap<String, String> environment = new HashMap<>();
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "DEFINITELY_NOT_A_REAL_ENV_VAR_NAME_XYZ");
        Assert.assertFalse(environment.containsKey("DEFINITELY_NOT_A_REAL_ENV_VAR_NAME_XYZ"));
    }

    @Test
    public void convertEnvironmentToEnvironSkipsInvalidValues() {
        HashMap<String, String> map = new HashMap<>();
        map.put("HAS_NULL_BYTE", "a\0b");
        Assert.assertTrue(ShellEnvironmentUtils.convertEnvironmentToEnviron(map).isEmpty());
    }

    @Test
    public void convertEnvironmentToDotEnvFileFromMapWritesExportLine() {
        HashMap<String, String> map = new HashMap<>();
        map.put("KEY", "value");
        Assert.assertEquals("export KEY=\"value\"\n",
            ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(map));
    }

    @Test
    public void convertEnvironmentToDotEnvFileFromMapIsEmptyForInvalidName() {
        HashMap<String, String> map = new HashMap<>();
        map.put("2BAD", "value");
        Assert.assertEquals("", ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(map));
    }

    @Test
    public void convertEnvironmentToDotEnvFileSkipsNullValuedVariables() {
        List<ShellEnvironmentVariable> list = new ArrayList<>();
        list.add(new ShellEnvironmentVariable("KEY", null, false));
        Assert.assertEquals("", ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(list));
    }
}
