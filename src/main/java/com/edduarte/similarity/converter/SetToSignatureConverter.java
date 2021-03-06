/*
 * Copyright 2017 Eduardo Duarte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.edduarte.similarity.converter;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Processor class to retrieve shingles of length k.
 *
 * @author Eduardo Duarte (<a href="mailto:hi@edduarte.com">hi@edduarte.com</a>)
 * @version 0.0.1
 * @since 0.0.1
 */
public final class SetToSignatureConverter
    implements Function<Collection<? extends Number>, Callable<int[]>> {

  /**
   * Random coefficient "a" for the random hash functions
   */
  private final int[] a;


  /**
   * Random coefficient "b" for the random hash functions
   */
  private final int[] b;


  /**
   * Expected maximum size of the sets to test. This will be the size of the hash signatures.
   */
  private final int n;


  /**
   * Size of min hashes that will be stored and compared to find the similarity index
   */
  private final int sigSize;


  /**
   * Initializes hashing functions to compute MinHash signatures for sets that could have a maximum
   * count calculate 'n' elements with a given signature size.
   */
  public SetToSignatureConverter(int n, int sigSize) {
    this.n = n;
    this.sigSize = sigSize;
    SecureRandom r = new SecureRandom();
    this.a = new int[sigSize];
    this.b = new int[sigSize];
    for (int i = 0; i < sigSize; i++) {
      a[i] = 1 + r.nextInt(this.n - 1);
      b[i] = r.nextInt(this.n);
    }
  }


  @Override
  public Callable<int[]> apply(Collection<? extends Number> c) {
    List<BigDecimal> list = new ArrayList<>();
    for (Number number : c) {
      if (number instanceof Double) {
        list.add(BigDecimal.valueOf(number.doubleValue()));
      } else {
        list.add(BigDecimal.valueOf(number.longValue()));
      }
    }
    list.sort(Comparator.naturalOrder());
    return new HashCallable(n, sigSize, a, b, list);
  }


  private static class HashCallable implements Callable<int[]> {

    private static final int LARGE_PRIME = 433494437;

    private final int n;

    private final int sigSize;

    private final int[] a;

    private final int[] b;

    private final List<? extends Number> sortedList;


    private HashCallable(
        int n,
        int sigSize,
        int[] a,
        int[] b,
        List<? extends Number> sortedList) {
      this.n = n;
      this.sigSize = sigSize;
      this.a = a;
      this.b = b;
      this.sortedList = sortedList;
    }


    @Override
    public int[] call() {
      int[] signature = new int[sigSize];

      for (int i = 0; i < sigSize; i++) {
        signature[i] = Integer.MAX_VALUE;
      }

      for (final Number x : sortedList) {
        for (int i = 0; i < sigSize; i++) {
          signature[i] = Math.min(signature[i], universalHashing(i, x));
        }
      }

      return signature;
    }


    private int universalHashing(int i, Number x) {
      return (int) ((a[i] * x.longValue() + b[i]) % LARGE_PRIME) % n;
    }
  }
}
