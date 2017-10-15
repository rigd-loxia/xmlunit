/*
  This file is licensed to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package org.xmlunit.placeholder;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.util.Nodes;

/**
 * This class is used to add placeholder feature to XML comparison. To use it, just add it with DiffBuilder like below <br><br>
 * <code>Diff diff = DiffBuilder.compare(control).withTest(test).withDifferenceEvaluator(new PlaceholderDifferenceEvaluator()).build();</code><br><br>
 * Supported scenarios are demonstrated in the unit tests (PlaceholderDifferenceEvaluatorTest).<br><br>
 * Default delimiters for placeholder are <code>${</code> and <code>}</code>. To use custom delimiters (in regular expression), create instance with the <code>PlaceholderDifferenceEvaluator(String placeholderOpeningDelimiterRegex, String placeholderClosingDelimiterRegex)</code> constructor. <br><br>
 * This class is <b>experimental/unstable</b>, hence the API or supported scenarios could change in future versions.<br><br>
 * @since 2.5.1
 */
public class PlaceholderDifferenceEvaluator implements DifferenceEvaluator {
    public static final String PLACEHOLDER_DEFAULT_OPENING_DELIMITER_REGEX = Pattern.quote("${");
    public static final String PLACEHOLDER_DEFAULT_CLOSING_DELIMITER_REGEX = Pattern.quote("}");
    private static final String PLACEHOLDER_PREFIX_REGEX = Pattern.quote("xmlunit.");
    private static final String PLACEHOLDER_NAME_IGNORE = "ignore";

    private final Pattern placeholderRegex;

    public PlaceholderDifferenceEvaluator() {
        this(null, null);
    }

    /**
     * Null, empty or whitespaces string argument is omitted. Otherwise, argument is trimmed.
     * @param placeholderOpeningDelimiterRegex opening delimiter of
     * placeholder, defaults to {@link
     * #PLACEHOLDER_DEFAULT_OPENING_DELIMITER_REGEX}
     * @param placeholderClosingDelimiterRegex closing delimiter of
     * placeholder, defaults to {@link
     * #PLACEHOLDER_DEFAULT_CLOSING_DELIMITER_REGEX}
     */
    public PlaceholderDifferenceEvaluator(String placeholderOpeningDelimiterRegex,
                                          String placeholderClosingDelimiterRegex) {
        if (placeholderOpeningDelimiterRegex == null
            || placeholderOpeningDelimiterRegex.trim().length() == 0) {
            placeholderOpeningDelimiterRegex = PLACEHOLDER_DEFAULT_OPENING_DELIMITER_REGEX;
        }
        if (placeholderClosingDelimiterRegex == null
            || placeholderClosingDelimiterRegex.trim().length() == 0) {
            placeholderClosingDelimiterRegex = PLACEHOLDER_DEFAULT_CLOSING_DELIMITER_REGEX;
        }

        placeholderRegex = Pattern.compile("(\\s*" + placeholderOpeningDelimiterRegex
            + "\\s*" + PLACEHOLDER_PREFIX_REGEX + "(.+)" + "\\s*"
            + placeholderClosingDelimiterRegex + "\\s*)");
    }

    public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.EQUAL) {
            return outcome;
        }

        Comparison.Detail controlDetails = comparison.getControlDetails();
        Node controlTarget = controlDetails.getTarget();
        Comparison.Detail testDetails = comparison.getTestDetails();
        Node testTarget = testDetails.getTarget();

        // comparing textual content of elements
        if (comparison.getType() == ComparisonType.TEXT_VALUE) {
            return evaluateConsideringPlaceholders((String) controlDetails.getValue(),
                (String) testDetails.getValue(), outcome);

        // "test document has no text-like child node but control document has"
        } else if (isMissingTextNodeDifference(comparison)) {
            return evaluateMissingTextNodeConsideringPlaceholders(comparison, outcome);

        // may be comparing TEXT to CDATA
        } else if (isTextCDATAMismatch(comparison)) {
            return evaluateConsideringPlaceholders(controlTarget.getNodeValue(), testTarget.getNodeValue(), outcome);

        // comparing textual content of attributes
        } else if (comparison.getType() == ComparisonType.ATTR_VALUE) {
            return evaluateConsideringPlaceholders((String) controlDetails.getValue(),
                (String) testDetails.getValue(), outcome);

        // two "test document has no attribute but control document has"
        } else if (isMissingAttributeDifference(comparison)) {
            return evaluateMissingAttributeConsideringPlaceholders(comparison, outcome);

        // default, don't apply any placeholders at all
        } else {
            return outcome;
        }
    }

    private boolean isMissingTextNodeDifference(Comparison comparison) {
        return controlHasOneTextChildAndTestHasNone(comparison)
            || cantFindControlTextChildInTest(comparison);
    }

    private boolean controlHasOneTextChildAndTestHasNone(Comparison comparison) {
        Comparison.Detail controlDetails = comparison.getControlDetails();
        Node controlTarget = controlDetails.getTarget();
        Comparison.Detail testDetails = comparison.getTestDetails();
        return comparison.getType() == ComparisonType.CHILD_NODELIST_LENGTH &&
            Integer.valueOf(1).equals(controlDetails.getValue()) &&
            Integer.valueOf(0).equals(testDetails.getValue()) &&
            isTextLikeNode(controlTarget.getFirstChild());
    }

    private boolean cantFindControlTextChildInTest(Comparison comparison) {
        Node controlTarget = comparison.getControlDetails().getTarget();
        return comparison.getType() == ComparisonType.CHILD_LOOKUP
            && controlTarget != null && isTextLikeNode(controlTarget);
    }

    private ComparisonResult evaluateMissingTextNodeConsideringPlaceholders(Comparison comparison, ComparisonResult outcome) {
        Node controlTarget = comparison.getControlDetails().getTarget();
        String value;
        if (controlHasOneTextChildAndTestHasNone(comparison)) {
            value = controlTarget.getFirstChild().getNodeValue();
        } else {
            value = controlTarget.getNodeValue();;
        }
        return evaluateConsideringPlaceholders(value, null, outcome);
    }

    private boolean isTextCDATAMismatch(Comparison comparison) {
        return comparison.getType() == ComparisonType.NODE_TYPE
            && isTextLikeNode(comparison.getControlDetails().getTarget())
            && isTextLikeNode(comparison.getTestDetails().getTarget());
    }

    private boolean isTextLikeNode(Node node) {
        short nodeType = node.getNodeType();
        return nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE;
    }

    private boolean isMissingAttributeDifference(Comparison comparison) {
        return comparison.getType() == ComparisonType.ELEMENT_NUM_ATTRIBUTES
            || (comparison.getType() == ComparisonType.ATTR_NAME_LOOKUP
                && comparison.getControlDetails().getTarget() != null
                && comparison.getControlDetails().getValue() != null);
    }

    private ComparisonResult evaluateMissingAttributeConsideringPlaceholders(Comparison comparison, ComparisonResult outcome) {
        if (comparison.getType() == ComparisonType.ELEMENT_NUM_ATTRIBUTES) {
            return evaluateAttributeListLengthConsideringPlaceholders(comparison, outcome);
        }
        String controlAttrValue = Nodes.getAttributes(comparison.getControlDetails().getTarget())
            .get((QName) comparison.getControlDetails().getValue());
        return evaluateConsideringPlaceholders(controlAttrValue, null, outcome);
    }

    private ComparisonResult evaluateAttributeListLengthConsideringPlaceholders(Comparison comparison,
        ComparisonResult outcome) {
        Map<QName, String> controlAttrs = Nodes.getAttributes(comparison.getControlDetails().getTarget());
        Map<QName, String> testAttrs = Nodes.getAttributes(comparison.getTestDetails().getTarget());

        int cAttrsMatched = 0;
        for (Map.Entry<QName, String> cAttr : controlAttrs.entrySet()) {
            String testValue = testAttrs.get(cAttr.getKey());
            if (testValue == null) {
                ComparisonResult o = evaluateConsideringPlaceholders(cAttr.getValue(), null, outcome);
                if (o != ComparisonResult.EQUAL) {
                    return outcome;
                }
            } else {
                cAttrsMatched++;
            }
        }
        if (cAttrsMatched != testAttrs.size()) {
            // there are unmatched test attributes
            return outcome;
        }
        return ComparisonResult.EQUAL;
    }

    private ComparisonResult evaluateConsideringPlaceholders(String controlText, String testText,
        ComparisonResult outcome) {
        Matcher m = placeholderRegex.matcher(controlText);
        if (m.find()) {
            String keyword = m.group(2).trim();
            if (isKnown(keyword)) {
                if (!m.group(1).trim().equals(controlText.trim())) {
                    throw new RuntimeException("The placeholder must exclusively occupy the text node.");
                }
                return evaluate(keyword, testText);
            }
        }

        // no placeholder at all or unknown keyword
        return outcome;
    }

    private boolean isKnown(String keyword) {
        return PLACEHOLDER_NAME_IGNORE.equals(keyword);
    }

    private ComparisonResult evaluate(String keyword, String testText) {
        // ignore placeholder
        return ComparisonResult.EQUAL;
    }
}
