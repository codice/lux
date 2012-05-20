/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package lux.xpath;

import lux.xquery.AttributeConstructor;
import lux.xquery.Conditional;
import lux.xquery.ElementConstructor;
import lux.xquery.FLWOR;
import lux.xquery.Let;
import lux.xquery.TextConstructor;
import lux.xquery.Variable;

public abstract class ExpressionVisitorBase extends ExpressionVisitor {

    @Override
    public AbstractExpression visit(PathStep step) {
        return step;
    }

    @Override
    public AbstractExpression visit(PathExpression path) {
        return path;
    }
    
    @Override
    public AbstractExpression visit(Let let) {
        return let;
    }
    
    @Override
    public AbstractExpression visit(Variable var) {
        return var;
    }

    @Override
    public AbstractExpression visit(Root root) {
        return root;
    }

    @Override
    public AbstractExpression visit(Dot dot) {
        return dot;
    }

    @Override
    public AbstractExpression visit(BinaryOperation op) {
        return op;
    }

    @Override
    public AbstractExpression visit(FunCall func) {
        return func;
    }

    @Override
    public AbstractExpression visit(LiteralExpression literal) {
        return literal;
    }

    @Override
    public AbstractExpression visit(Predicate predicate) {
        return predicate;
    }

    @Override
    public AbstractExpression visit(Sequence seq) {
        return seq;
    }
    
    @Override
    public AbstractExpression visit(Subsequence subseq) {
        return subseq;
    }

    @Override
    public AbstractExpression visit(SetOperation setop) {
        return setop;
    }

    @Override
    public AbstractExpression visit(UnaryMinus unaryMinus) {
        return unaryMinus;
    }
    
    @Override
    public AbstractExpression visit(ElementConstructor elementConstructor) {
        return elementConstructor;
    }

    @Override
    public AbstractExpression visit(AttributeConstructor attributeConstructor) {
        return attributeConstructor;
    }
    
    @Override
    public AbstractExpression visit(TextConstructor textConstructor) {
        return textConstructor;
    }
    
    @Override
    public AbstractExpression visit(FLWOR flwor) {
        return flwor;
    }
    
    @Override
    public AbstractExpression visit(Conditional cond) {
        return cond;
    }
}
